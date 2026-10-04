package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.CaminhaoRequest;
import com.logitech.sgfl.dto.FurgaoRequest;
import com.logitech.sgfl.dto.VeiculoResponse;
import com.logitech.sgfl.config.Pagination;
import com.logitech.sgfl.dto.PageResponse;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.me.Caminhao;
import com.logitech.sgfl.me.Furgao;
import com.logitech.sgfl.me.Veiculo;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.repository.VeiculoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

    private final VeiculoRepository veiculoRepository;
    private final EntregaRepository entregaRepository;

    public VeiculoController(
            VeiculoRepository veiculoRepository,
            EntregaRepository entregaRepository
    ) {
        this.veiculoRepository = veiculoRepository;
        this.entregaRepository = entregaRepository;
    }

    @GetMapping
    public PageResponse<VeiculoResponse> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        Pageable pageable = Pagination.request(page, size);
        String termo = limitarTermo(q);
        Page<Veiculo> veiculos = termo == null || termo.isBlank()
                ? veiculoRepository.findAll(pageable)
                : veiculoRepository.findByPlacaContainingIgnoreCaseOrModeloContainingIgnoreCase(termo, termo, pageable);
        return PageResponse.from(veiculos, VeiculoResponse::from);
    }

    private String limitarTermo(String q) {
        if (q == null) return null;
        String termo = q.trim();
        return termo.substring(0, Math.min(termo.length(), 100));
    }

    @PostMapping("/caminhao")
    public ResponseEntity<VeiculoResponse> criarCaminhao(@Valid @RequestBody CaminhaoRequest request) {
        String placa = normalizarPlaca(request.getPlaca());
        validarPlacaNormalizada(placa);

        if (veiculoRepository.existsByPlaca(placa)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Caminhao caminhao = new Caminhao(
                placa, request.getModelo().trim(), request.getCapacidadeCargaKg(), request.getQuantidadeEixos());

        return ResponseEntity.status(HttpStatus.CREATED).body(VeiculoResponse.from(veiculoRepository.save(caminhao)));
    }

    @PostMapping("/furgao")
    public ResponseEntity<VeiculoResponse> criarFurgao(@Valid @RequestBody FurgaoRequest request) {
        String placa = normalizarPlaca(request.getPlaca());
        validarPlacaNormalizada(placa);

        if (veiculoRepository.existsByPlaca(placa)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Furgao furgao = new Furgao(
                placa, request.getModelo().trim(), request.getCapacidadeCargaKg(), request.getVolumeM3());

        return ResponseEntity.status(HttpStatus.CREATED).body(VeiculoResponse.from(veiculoRepository.save(furgao)));
    }

    @PutMapping("/caminhao/{id}")
    public ResponseEntity<VeiculoResponse> atualizarCaminhao(
            @PathVariable Long id,
            @Valid @RequestBody CaminhaoRequest request
    ) {
        Veiculo existente = veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + id));

        if (!(existente instanceof Caminhao caminhao)) {
            throw new IllegalArgumentException("O veículo informado não é um Caminhão.");
        }

        String placa = normalizarPlaca(request.getPlaca());
        validarPlacaNormalizada(placa);

        if (!placa.equals(caminhao.getPlaca()) && veiculoRepository.existsByPlaca(placa)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        caminhao.setPlaca(placa);
        caminhao.setModelo(request.getModelo().trim());
        caminhao.setCapacidadeCargaKg(request.getCapacidadeCargaKg());
        caminhao.setQuantidadeEixos(request.getQuantidadeEixos());

        return ResponseEntity.ok(VeiculoResponse.from(veiculoRepository.save(caminhao)));
    }

    @PutMapping("/furgao/{id}")
    public ResponseEntity<VeiculoResponse> atualizarFurgao(
            @PathVariable Long id,
            @Valid @RequestBody FurgaoRequest request
    ) {
        Veiculo existente = veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + id));

        if (!(existente instanceof Furgao furgao)) {
            throw new IllegalArgumentException("O veículo informado não é um Furgão.");
        }

        String placa = normalizarPlaca(request.getPlaca());
        validarPlacaNormalizada(placa);

        if (!placa.equals(furgao.getPlaca()) && veiculoRepository.existsByPlaca(placa)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        furgao.setPlaca(placa);
        furgao.setModelo(request.getModelo().trim());
        furgao.setCapacidadeCargaKg(request.getCapacidadeCargaKg());
        furgao.setVolumeM3(request.getVolumeM3());

        return ResponseEntity.ok(VeiculoResponse.from(veiculoRepository.save(furgao)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirVeiculo(@PathVariable Long id) {
        if (!veiculoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Veículo não encontrado: " + id);
        }

        // Não permitimos excluir um veículo que esteja relacionado a alguma entrega.
        if (entregaRepository.existsByVeiculo_Id(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        veiculoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // O formato da placa (7 caracteres após remover pontuação/traço) é uma regra
    // de negócio específica, não coberta por uma anotação simples de Bean
    // Validation — por isso continua como validação manual, mesmo com o DTO
    // cuidando da presença/positividade dos demais campos.
    private void validarPlacaNormalizada(String placa) {
        if (placa.length() != 7) {
            throw new IllegalArgumentException("A placa deve possuir 7 caracteres.");
        }
    }

    private String normalizarPlaca(String placa) {
        return placa.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }
}
