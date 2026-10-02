package com.logitech.sgfl.repository;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long>, JpaSpecificationExecutor<Entrega> {

    boolean existsByIdAndStatus(Long id, StatusEntrega status);

    boolean existsByMotorista_Id(Long motoristaId);

    boolean existsByVeiculo_Id(Long veiculoId);

    boolean existsByMotorista_IdAndStatus(
            Long motoristaId,
            StatusEntrega status
    );

    boolean existsByVeiculo_IdAndStatus(
            Long veiculoId,
            StatusEntrega status
    );
}
