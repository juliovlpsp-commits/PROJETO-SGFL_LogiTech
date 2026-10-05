package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.EntregaEta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregaEtaRepository extends JpaRepository<EntregaEta, Long> {

    Optional<EntregaEta> findTopByEntregaIdOrderByCriadoEmDesc(Long entregaId);

    List<EntregaEta> findByEntregaIdOrderByCriadoEmDesc(Long entregaId);
}
