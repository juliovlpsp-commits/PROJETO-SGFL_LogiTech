package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.ClienteRequest;
import com.logitech.sgfl.dto.ClienteResponse;
import com.logitech.sgfl.config.Pagination;
import com.logitech.sgfl.dto.PageResponse;
import com.logitech.sgfl.me.Cliente;
import com.logitech.sgfl.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public PageResponse<ClienteResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Cliente> clientes = clienteService.listar(Pagination.request(page, size));
        return PageResponse.from(clientes, ClienteResponse::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscar(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ClienteResponse.from(clienteService.buscar(id))
        );
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> criar(
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
                .body(ClienteResponse.from(cliente));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request,
            @RequestParam(
                    defaultValue = "true"
            )
            boolean ativo
    ) {

        return ResponseEntity.ok(
                ClienteResponse.from(clienteService.atualizar(
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
                ))
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
