package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    boolean existsByPlaca(String placa);

    Page<Veiculo> findByPlacaContainingIgnoreCaseOrModeloContainingIgnoreCase(
            String placa, String modelo, Pageable pageable);
}
