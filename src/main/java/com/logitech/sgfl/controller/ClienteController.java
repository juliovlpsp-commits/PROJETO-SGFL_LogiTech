package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.ClienteRequest;
import com.logitech.sgfl.me.Cliente;
import com.logitech.sgfl.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(
            ClienteService clienteService
    ) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<List<Cliente>> listar() {
        return ResponseEntity.ok(
                clienteService.listar()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscar(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                clienteService.buscar(id)
        );
    }

    @PostMapping
    public ResponseEntity<Cliente> criar(
            @Valid @RequestBody ClienteRequest request
    ) {

        Cliente cliente =
                clienteService.criar(
                        request.getNome(),
                        request.getCpf(),
                        request.getEmail(),
                        request.getTelefone(),
                        request.getCep(),
                        request.getLogradouro(),
                        request.getNumero(),
                        request.getComplemento(),
                        request.getBairro(),
                        request.getCidade(),
                        request.getUf()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cliente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request,
            @RequestParam(
                    defaultValue = "true"
            )
            boolean ativo
    ) {

        return ResponseEntity.ok(
                clienteService.atualizar(
                        id,
                        request.getNome(),
                        request.getCpf(),
                        request.getEmail(),
                        request.getTelefone(),
                        request.getCep(),
                        request.getLogradouro(),
                        request.getNumero(),
                        request.getComplemento(),
                        request.getBairro(),
                        request.getCidade(),
                        request.getUf(),
                        ativo
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id
    ) {

        clienteService.excluir(id);

        return ResponseEntity.noContent()
                .build();
    }
}