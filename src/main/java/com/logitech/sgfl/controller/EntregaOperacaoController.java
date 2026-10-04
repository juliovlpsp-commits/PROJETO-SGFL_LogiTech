package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.ComprovanteEntregaRequest;
import com.logitech.sgfl.dto.ComprovanteEntregaResponse;
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
            String contentType = foto.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RegraNegocioException("O comprovante fotográfico deve ser uma imagem.");
            }
            String ext = switch (contentType) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
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
}
