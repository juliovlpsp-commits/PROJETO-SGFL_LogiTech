package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.ComprovanteEntregaRequest;
import com.logitech.sgfl.dto.ComprovanteEntregaResponse;
import com.logitech.sgfl.dto.CoordenadasEntregaRequest;
import com.logitech.sgfl.dto.EntregaResponse;
import com.logitech.sgfl.dto.EntregaTimelineResponse;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.exceptions.RegraNegocioException;
import com.logitech.sgfl.me.ComprovanteEntrega;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.repository.ComprovanteEntregaRepository;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.service.EntregaAuditoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/entregas")
public class EntregaOperacaoController {
    private final EntregaRepository entregaRepository;
    private final ComprovanteEntregaRepository comprovanteRepository;
    private final EntregaAuditoriaService auditoriaService;
    private final Path pastaUploads;

    public EntregaOperacaoController(
            EntregaRepository entregaRepository,
            ComprovanteEntregaRepository comprovanteRepository,
            EntregaAuditoriaService auditoriaService,
            @Value("${SGFL_UPLOAD_DIR:uploads/comprovantes}") String uploadDir
    ) throws IOException {
        this.entregaRepository = entregaRepository;
        this.comprovanteRepository = comprovanteRepository;
        this.auditoriaService = auditoriaService;
        this.pastaUploads = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.pastaUploads);
    }

    @GetMapping("/{id}/timeline")
    public List<EntregaTimelineResponse> timeline(@PathVariable Long id) {
        if (!entregaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Entrega não encontrada: " + id);
        }
        return auditoriaService.timeline(id).stream()
                .map(EntregaTimelineResponse::from)
                .toList();
    }

    @GetMapping("/{id}/comprovante")
    public ResponseEntity<ComprovanteEntregaResponse> comprovante(@PathVariable Long id) {
        ComprovanteEntrega comprovante = comprovanteRepository.findByEntrega_Id(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comprovante de entrega não encontrado."));
        return ResponseEntity.ok(ComprovanteEntregaResponse.from(comprovante));
    }

    @PostMapping(value = "/{id}/comprovante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComprovanteEntregaResponse> salvarComprovante(
            @PathVariable Long id,
            @RequestPart("dados") @Valid ComprovanteEntregaRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto
    ) throws IOException {
        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega não encontrada: " + id));

        if (entrega.getStatus() != com.logitech.sgfl.enums.StatusEntrega.EM_TRANSITO) {
            throw new RegraNegocioException("O comprovante só pode ser registrado para uma entrega EM_TRANSITO.");
        }

        ComprovanteEntrega comprovante = comprovanteRepository.findByEntrega_Id(id)
                .orElseGet(ComprovanteEntrega::new);
        comprovante.setEntrega(entrega);
        comprovante.setNomeRecebedor(request.nomeRecebedor().trim());
        comprovante.setAssinatura(request.assinatura());
        comprovante.setRecebidoEm(request.recebidoEm() == null ? LocalDateTime.now() : request.recebidoEm());
        comprovante.setObservacao(request.observacao());

        if (foto != null && !foto.isEmpty()) {
            if (foto.getSize() > 5L * 1024 * 1024) {
                throw new RegraNegocioException("A foto do comprovante deve ter no máximo 5 MB.");
            }
            String contentType = foto.getContentType();
            String ext = switch (contentType == null ? "" : contentType.toLowerCase(java.util.Locale.ROOT)) {
                case "image/png" -> {
                    if (!temAssinatura(foto, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
                        throw new RegraNegocioException("O conteúdo não corresponde a uma imagem PNG válida.");
                    }
                    yield ".png";
                }
                case "image/jpeg", "image/jpg" -> {
                    if (!temAssinatura(foto, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
                        throw new RegraNegocioException("O conteúdo não corresponde a uma imagem JPEG válida.");
                    }
                    yield ".jpg";
                }
                case "image/webp" -> {
                    if (!temAssinatura(foto, new byte[]{'R', 'I', 'F', 'F'}) || !temAssinaturaNoOffset(foto, 8, new byte[]{'W', 'E', 'B', 'P'})) {
                        throw new RegraNegocioException("O conteúdo não corresponde a uma imagem WebP válida.");
                    }
                    yield ".webp";
                }
                default -> throw new RegraNegocioException("Envie uma imagem PNG, JPEG ou WebP válida.");
            };
            String nomeArquivo = "entrega-" + id + "-" + UUID.randomUUID() + ext;
            Path destino = pastaUploads.resolve(nomeArquivo).normalize();
            if (!destino.getParent().equals(pastaUploads)) {
                throw new RegraNegocioException("Caminho de arquivo inválido.");
            }
            foto.transferTo(destino);
            comprovante.setFotoPath(nomeArquivo);
        }

        ComprovanteEntrega salvo = comprovanteRepository.save(comprovante);
        auditoriaService.registrar(entrega, "COMPROVANTE_ANEXADO", entrega.getStatus(), entrega.getStatus(),
                "Comprovante registrado por " + request.nomeRecebedor().trim());
        return ResponseEntity.ok(ComprovanteEntregaResponse.from(salvo));
    }

    private boolean temAssinatura(MultipartFile file, byte[] expected) throws IOException {
        return temAssinaturaNoOffset(file, 0, expected);
    }

    private boolean temAssinaturaNoOffset(MultipartFile file, int offset, byte[] expected) throws IOException {
        try (var input = file.getInputStream()) {
            if (input.skip(offset) != offset) return false;
            byte[] actual = input.readNBytes(expected.length);
            return java.util.Arrays.equals(actual, expected);
        }
    }

    /**
     * Atualiza as coordenadas de rota da entrega (geocodificação dos
     * endereços). É o que liga a entrega ao cálculo de rota/ETA e ao
     * mapa: sem coordenadas o sistema só consegue estimar em linha reta.
     */
    @PutMapping("/{id}/coordenadas")
    public ResponseEntity<EntregaResponse> atualizarCoordenadas(
            @PathVariable Long id,
            @Valid @RequestBody CoordenadasEntregaRequest request
    ) {

        /*
         * Valida o payload antes de tocar no banco: entrada inválida
         * devolve 400 mesmo quando a entrega não existe.
         */
        boolean temOrigem = request.latitudeOrigem() != null || request.longitudeOrigem() != null;
        boolean temDestino = request.latitudeDestino() != null || request.longitudeDestino() != null;

        if (!temOrigem && !temDestino) {
            throw new RegraNegocioException(
                    "Informe ao menos a origem ou o destino da rota.");
        }

        if ((request.latitudeOrigem() == null) != (request.longitudeOrigem() == null)) {
            throw new RegraNegocioException(
                    "Informe latitude e longitude da origem juntas.");
        }

        if ((request.latitudeDestino() == null) != (request.longitudeDestino() == null)) {
            throw new RegraNegocioException(
                    "Informe latitude e longitude do destino juntas.");
        }

        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega não encontrada: " + id));

        if (temOrigem) {
            entrega.setLatitudeOrigem(request.latitudeOrigem());
            entrega.setLongitudeOrigem(request.longitudeOrigem());
        }

        if (temDestino) {
            entrega.setLatitudeDestino(request.latitudeDestino());
            entrega.setLongitudeDestino(request.longitudeDestino());
        }

        Entrega salva = entregaRepository.save(entrega);

        auditoriaService.registrar(
                salva,
                "COORDENADAS_ATUALIZADAS",
                salva.getStatus(),
                salva.getStatus(),
                "Coordenadas de rota atualizadas pelo operador."
        );

        return ResponseEntity.ok(EntregaResponse.from(salva));
    }
}
