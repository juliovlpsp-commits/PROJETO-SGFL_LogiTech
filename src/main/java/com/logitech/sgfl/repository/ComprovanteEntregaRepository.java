package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.ComprovanteEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComprovanteEntregaRepository extends JpaRepository<ComprovanteEntrega, Long> {
    Optional<ComprovanteEntrega> findByEntrega_Id(Long entregaId);
}
