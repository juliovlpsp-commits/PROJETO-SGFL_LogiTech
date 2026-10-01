package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.PedidoRequest;
import com.logitech.sgfl.me.Pedido;
import com.logitech.sgfl.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<Pedido>> listar() {

        return ResponseEntity.ok(
                pedidoService.listar()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscar(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                pedidoService.buscar(id)
        );
    }

    @PostMapping
    public ResponseEntity<Pedido> criar(
            @Valid @RequestBody PedidoRequest request
    ) {

        Pedido pedido =
                pedidoService.criar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pedido);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Pedido> cancelar(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                pedidoService.cancelar(id)
        );
    }
}