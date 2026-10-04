package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.EstoqueRequest;
import com.logitech.sgfl.dto.ProdutoRequest;
import com.logitech.sgfl.dto.ProdutoResponse;
import com.logitech.sgfl.config.Pagination;
import com.logitech.sgfl.dto.PageResponse;
import com.logitech.sgfl.me.Produto;
import com.logitech.sgfl.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public PageResponse<ProdutoResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Produto> produtos = produtoService.listar(Pagination.request(page, size));
        return PageResponse.from(produtos, ProdutoResponse::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscar(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ProdutoResponse.from(produtoService.buscar(id))
        );
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(
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
                .body(ProdutoResponse.from(produto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoRequest request
    ) {

        return ResponseEntity.ok(
                ProdutoResponse.from(produtoService.atualizar(
                        id,
                        request.getCodigo(),
                        request.getNome(),
                        request.getDescricao(),
                        request.getPreco(),
                        request.isAtivo()
                ))
        );
    }

    @PutMapping("/{id}/estoque")
    public ResponseEntity<ProdutoResponse> atualizarEstoque(
            @PathVariable Long id,
            @Valid @RequestBody EstoqueRequest request
    ) {

        return ResponseEntity.ok(
                ProdutoResponse.from(produtoService.atualizarEstoque(
                        id,
                        request.getQuantidade()
                ))
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
