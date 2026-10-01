package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.EstoqueRequest;
import com.logitech.sgfl.dto.ProdutoRequest;
import com.logitech.sgfl.me.Produto;
import com.logitech.sgfl.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(
            ProdutoService produtoService
    ) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public ResponseEntity<List<Produto>> listar() {

        return ResponseEntity.ok(
                produtoService.listar()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produto> buscar(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                produtoService.buscar(id)
        );
    }

    @PostMapping
    public ResponseEntity<Produto> criar(
            @Valid @RequestBody ProdutoRequest request
    ) {

        Produto produto =
                produtoService.criar(
                        request.getCodigo(),
                        request.getNome(),
                        request.getDescricao(),
                        request.getPreco(),
                        request.getQuantidadeEstoqueInicial(),
                        request.isAtivo()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(produto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produto> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoRequest request
    ) {

        return ResponseEntity.ok(
                produtoService.atualizar(
                        id,
                        request.getCodigo(),
                        request.getNome(),
                        request.getDescricao(),
                        request.getPreco(),
                        request.isAtivo()
                )
        );
    }

    @PutMapping("/{id}/estoque")
    public ResponseEntity<Produto> atualizarEstoque(
            @PathVariable Long id,
            @Valid @RequestBody EstoqueRequest request
    ) {

        return ResponseEntity.ok(
                produtoService.atualizarEstoque(
                        id,
                        request.getQuantidade()
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id
    ) {

        produtoService.excluir(id);

        return ResponseEntity.noContent()
                .build();
    }
}