package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.RastreioPublicoResponse;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.repository.EntregaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rastreio")
public class RastreioPublicoController {
    private final EntregaRepository repository;

    public RastreioPublicoController(EntregaRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<RastreioPublicoResponse> buscar(@PathVariable String codigo) {
        Entrega entrega = repository.findByCodigoRastreio(codigo.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Código de rastreio não encontrado."));
        return ResponseEntity.ok(RastreioPublicoResponse.from(entrega));
    }
}
