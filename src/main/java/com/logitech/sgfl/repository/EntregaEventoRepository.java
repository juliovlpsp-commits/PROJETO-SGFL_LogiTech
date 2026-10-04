package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.EntregaEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntregaEventoRepository extends JpaRepository<EntregaEvento, Long> {
    List<EntregaEvento> findByEntrega_IdOrderByOcorridoEmAsc(Long entregaId);
}
