package com.logitech.sgfl.controller;

import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.me.Motorista;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.repository.MotoristaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<Motorista>> listarTodos() {
        return ResponseEntity.ok(
                motoristaRepository.findAll()
        );
    }

    @PostMapping
    public ResponseEntity<Motorista> criarMotorista(
            @RequestBody Motorista motorista
    ) {
        validarMotorista(motorista);

        String cpf = normalizarCpf(
                motorista.getCpf()
        );

        if (motoristaRepository.existsByCpf(cpf)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        motorista.setCpf(cpf);

        Motorista salvo =
                motoristaRepository.save(motorista);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(salvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Motorista> atualizarMotorista(
            @PathVariable Long id,
            @RequestBody Motorista dados
    ) {
        validarMotorista(dados);

        Motorista motorista =
                motoristaRepository.findById(id)
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Motorista não encontrado: " + id
                                )
                        );

        String cpf =
                normalizarCpf(dados.getCpf());

        /*
         * Só verifica duplicidade se o CPF realmente
         * estiver sendo alterado.
         */
        if (!cpf.equals(motorista.getCpf()) &&
                motoristaRepository.existsByCpf(cpf)) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        motorista.setNome(
                dados.getNome().trim()
        );

        motorista.setCpf(cpf);

        motorista.setTipoCNH(
                dados.getTipoCNH()
        );

        return ResponseEntity.ok(
                motoristaRepository.save(motorista)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirMotorista(
            @PathVariable Long id
    ) {
        if (!motoristaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                    "Motorista não encontrado: " + id
            );
        }

        /*
         * Não permitimos apagar um motorista que esteja
         * referenciado por uma entrega.
         */
        if (entregaRepository.existsByMotorista_Id(id)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        motoristaRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private void validarMotorista(
            Motorista motorista
    ) {

        if (motorista == null) {
            throw new IllegalArgumentException(
                    "Os dados do motorista são obrigatórios."
            );
        }

        if (motorista.getNome() == null ||
                motorista.getNome().isBlank()) {

            throw new IllegalArgumentException(
                    "O nome do motorista é obrigatório."
            );
        }

        if (motorista.getCpf() == null ||
                motorista.getCpf().isBlank()) {

            throw new IllegalArgumentException(
                    "O CPF do motorista é obrigatório."
            );
        }

        if (motorista.getTipoCNH() == null) {

            throw new IllegalArgumentException(
                    "O tipo da CNH é obrigatório."
            );
        }

        String cpf =
                normalizarCpf(motorista.getCpf());

        if (cpf.length() != 11) {

            throw new IllegalArgumentException(
                    "O CPF deve conter 11 dígitos."
            );
        }
    }

    private String normalizarCpf(
            String cpf
    ) {
        return cpf
                .replaceAll("\\D", "");
    }
}