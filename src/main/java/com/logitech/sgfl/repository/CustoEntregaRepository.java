package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.CustoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface CustoEntregaRepository extends JpaRepository<CustoEntrega, Long> {
    List<CustoEntrega> findByEntrega_IdOrderByCriadoEmAsc(Long entregaId);

}
