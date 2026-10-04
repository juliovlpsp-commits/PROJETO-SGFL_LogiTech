package com.logitech.sgfl.repository;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long>, JpaSpecificationExecutor<Entrega> {

    /** Fetches relations read by EntregaResponse before the service transaction closes. */
    @Override
    @EntityGraph(attributePaths = {"veiculo", "motorista"})
    Optional<Entrega> findById(Long id);

    /** Fetches the two to-one relations used by EntregaResponse for the entire page. */
    @Override
    @EntityGraph(attributePaths = {"veiculo", "motorista"})
    Page<Entrega> findAll(Specification<Entrega> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"veiculo", "motorista"})
    java.util.Optional<Entrega> findByCodigoRastreio(String codigoRastreio);

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
