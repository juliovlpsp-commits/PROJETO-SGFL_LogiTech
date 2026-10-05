package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.EntregaEtaResponse;
import com.logitech.sgfl.dto.RotaEstimativaResponse;
import com.logitech.sgfl.service.RotaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entregas")
public class RotaController {
    private final RotaService service;

    public RotaController(RotaService service) {
        this.service = service;
    }

    @GetMapping("/{id}/rota")
    public RotaEstimativaResponse rota(@PathVariable Long id) {
        return service.estimar(id);
    }

    /**
     * Histórico de previsões (ETA) já calculadas para a entrega.
     */
    @GetMapping("/{id}/rota/historico")
    public List<EntregaEtaResponse> historico(@PathVariable Long id) {
        return service.historico(id);
    }
}
