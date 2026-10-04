package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.Pedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {

    boolean existsByCliente_Id(
            Long clienteId
    );

    @Query("""
            select distinct p
            from Pedido p
            join fetch p.cliente
            left join fetch p.itens i
            left join fetch i.produto
            order by p.id desc
            """)
    List<Pedido> findAllComItens();

    @Query("""
            select distinct p
            from Pedido p
            join fetch p.cliente
            left join fetch p.itens i
            left join fetch i.produto
            where p.id = :id
            """)
    Optional<Pedido> findByIdComItens(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct p
            from Pedido p
            join fetch p.cliente
            left join fetch p.itens i
            left join fetch i.produto
            where p.id = :id
            """)
    Optional<Pedido> findByIdComItensForUpdate(
            @Param("id") Long id
    );
}