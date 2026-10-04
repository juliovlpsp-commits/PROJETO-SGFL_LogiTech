package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.*;
import com.logitech.sgfl.me.CustoEntrega;
import com.logitech.sgfl.service.OperacionalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operacional")
public class OperacionalController {
    private final OperacionalService service;

    public OperacionalController(OperacionalService service) {
        this.service = service;
    }

    @GetMapping("/kpis")
    public OperacionalKpiResponse kpis() {
        return service.kpis();
    }

    @GetMapping("/alertas")
    public List<AlertaOperacionalResponse> alertas() {
        return service.alertas();
    }

    @PostMapping("/entregas/{entregaId}/custos")
    public ResponseEntity<CustoEntregaResponse> adicionarCusto(
            @PathVariable Long entregaId,
            @Valid @RequestBody CustoEntregaRequest request
    ) {
        CustoEntrega custo = service.adicionarCusto(entregaId, request);
        return ResponseEntity.ok(CustoEntregaResponse.from(custo));
    }

    @GetMapping("/entregas/{entregaId}/custos")
    public List<CustoEntregaResponse> custos(@PathVariable Long entregaId) {
        return service.custos(entregaId).stream().map(CustoEntregaResponse::from).toList();
    }
}
