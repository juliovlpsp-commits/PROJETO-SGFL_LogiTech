package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.RastreioPublicoResponse;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.service.RotaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rastreio")
public class RastreioPublicoController {
    private final EntregaRepository repository;
    private final RotaService rotaService;

    public RastreioPublicoController(EntregaRepository repository, RotaService rotaService) {
        this.repository = repository;
        this.rotaService = rotaService;
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<RastreioPublicoResponse> buscar(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "true") boolean incluirRota
    ) {
        Entrega entrega = repository.findByCodigoRastreio(codigo.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Código de rastreio não encontrado."));
        RastreioPublicoResponse response = RastreioPublicoResponse.from(entrega);
        if (incluirRota && entrega.getStatus() == StatusEntrega.EM_TRANSITO
                && entrega.getLatitudeOrigem() != null && entrega.getLongitudeOrigem() != null
                && entrega.getLatitudeDestino() != null && entrega.getLongitudeDestino() != null) {
            response = response.comRota(rotaService.estimar(entrega.getId()));
        }
        return ResponseEntity.ok(response);
    }
}
