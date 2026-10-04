package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.PedidoRequest;
import com.logitech.sgfl.dto.PedidoResponse;
import com.logitech.sgfl.config.Pagination;
import com.logitech.sgfl.dto.PageResponse;
import com.logitech.sgfl.me.Pedido;
import com.logitech.sgfl.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(
            PedidoService pedidoService
    ) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public PageResponse<PedidoResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Pedido> pedidos = pedidoService.listar(Pagination.request(page, size));
        return PageResponse.from(pedidos, PedidoResponse::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscar(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                PedidoResponse.from(pedidoService.buscar(id))
        );
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(
            @Valid @RequestBody PedidoRequest request
    ) {

        Pedido pedido =
                pedidoService.criar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PedidoResponse.from(pedido));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponse> cancelar(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                PedidoResponse.from(pedidoService.cancelar(id))
        );
    }
}
