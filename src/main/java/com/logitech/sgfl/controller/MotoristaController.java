package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.MotoristaRequest;
import com.logitech.sgfl.dto.MotoristaResponse;
import com.logitech.sgfl.config.Pagination;
import com.logitech.sgfl.dto.PageResponse;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.me.Motorista;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.repository.MotoristaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/motoristas")
public class MotoristaController {

    private final MotoristaRepository motoristaRepository;
    private final EntregaRepository entregaRepository;

    public MotoristaController(
            MotoristaRepository motoristaRepository,
            EntregaRepository entregaRepository
    ) {
        this.motoristaRepository = motoristaRepository;
        this.entregaRepository = entregaRepository;
    }

    @GetMapping
    public PageResponse<MotoristaResponse> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        Pageable pageable = Pagination.request(page, size);
        String termo = limitarTermo(q);
        Page<Motorista> motoristas = termo == null || termo.isBlank()
                ? motoristaRepository.findAll(pageable)
                : motoristaRepository.findByNomeContainingIgnoreCaseOrCpfContaining(termo, termo, pageable);
        return PageResponse.from(motoristas, MotoristaResponse::from);
    }

    private String limitarTermo(String q) {
        if (q == null) return null;
        String termo = q.trim();
        return termo.substring(0, Math.min(termo.length(), 100));
    }

    @PostMapping
    public ResponseEntity<MotoristaResponse> criarMotorista(@Valid @RequestBody MotoristaRequest request) {
        String cpf = normalizarCpf(request.getCpf());
        validarCpfNormalizado(cpf);

        if (motoristaRepository.existsByCpf(cpf)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Motorista motorista = new Motorista(request.getNome().trim(), cpf, request.getTipoCNH());
        Motorista salvo = motoristaRepository.save(motorista);

        return ResponseEntity.status(HttpStatus.CREATED).body(MotoristaResponse.from(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MotoristaResponse> atualizarMotorista(
            @PathVariable Long id,
            @Valid @RequestBody MotoristaRequest request
    ) {
        Motorista motorista = motoristaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Motorista não encontrado: " + id));

        String cpf = normalizarCpf(request.getCpf());
        validarCpfNormalizado(cpf);

        // Só verifica duplicidade se o CPF realmente estiver sendo alterado.
        if (!cpf.equals(motorista.getCpf()) && motoristaRepository.existsByCpf(cpf)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        motorista.setNome(request.getNome().trim());
        motorista.setCpf(cpf);
        motorista.setTipoCNH(request.getTipoCNH());

        return ResponseEntity.ok(MotoristaResponse.from(motoristaRepository.save(motorista)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirMotorista(@PathVariable Long id) {
        if (!motoristaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Motorista não encontrado: " + id);
        }

        // Não permitimos apagar um motorista que esteja referenciado por uma entrega.
        if (entregaRepository.existsByMotorista_Id(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        motoristaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Além do tamanho, valida os dois dígitos verificadores e rejeita sequências repetidas
    // (000.000.000-00, 111.111.111-11, ...), que passam na conta mas não são CPFs reais.
    private void validarCpfNormalizado(String cpf) {
        if (cpf.length() != 11) {
            throw new IllegalArgumentException("O CPF deve conter 11 dígitos.");
        }
        if (!cpfPossuiDigitosVerificadoresValidos(cpf)) {
            throw new IllegalArgumentException("CPF inválido.");
        }
    }

    static boolean cpfPossuiDigitosVerificadoresValidos(String cpf) {
        if (cpf == null || cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(cpf, 9) == Character.getNumericValue(cpf.charAt(9))
                && digitoVerificador(cpf, 10) == Character.getNumericValue(cpf.charAt(10));
    }

    private static int digitoVerificador(String cpf, int quantidadeDigitos) {
        int soma = 0;
        for (int i = 0; i < quantidadeDigitos; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (quantidadeDigitos + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    private String normalizarCpf(String cpf) {
        return cpf.replaceAll("\\D", "");
    }
}
