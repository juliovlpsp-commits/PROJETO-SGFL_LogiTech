package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.GeocodificacaoResponse;
import com.logitech.sgfl.service.GeocodificacaoService;
import org.springframework.web.bind.annotation.*;

/**
 * Geocodificação endereço <-> coordenada.
 *
 * Restrito a usuário autenticado (SecurityConfig: qualquer /api/**).
 */
@RestController
@RequestMapping("/api/geocodificacao")
public class GeocodificacaoController {

    private final GeocodificacaoService service;

    public GeocodificacaoController(GeocodificacaoService service) {
        this.service = service;
    }

    /**
     * Endereço -> coordenadas.
     */
    @GetMapping
    public GeocodificacaoResponse geocodificar(
            @RequestParam String endereco
    ) {
        return service.geocodificar(endereco);
    }

    /**
     * Coordenadas -> endereço.
     */
    @GetMapping("/reversa")
    public GeocodificacaoResponse reversa(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return service.reverter(latitude, longitude);
    }
}
