package com.logitech.sgfl.service;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.me.EntregaEvento;
import com.logitech.sgfl.repository.EntregaEventoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EntregaAuditoriaService {
    private final EntregaEventoRepository repository;

    public EntregaAuditoriaService(EntregaEventoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(Entrega entrega, String tipo, StatusEntrega anterior,
                          StatusEntrega novo, String observacao) {
        repository.save(new EntregaEvento(
                entrega,
                tipo,
                anterior,
                novo,
                LocalDateTime.now(),
                responsavelAtual(),
                observacao
        ));
    }

    @Transactional(readOnly = true)
    public List<EntregaEvento> timeline(Long entregaId) {
        return repository.findByEntrega_IdOrderByOcorridoEmAsc(entregaId);
    }

    private String responsavelAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "sistema";
        }
        String nome = authentication.getName();
        return nome == null || nome.isBlank() ? "sistema" : nome;
    }
}
