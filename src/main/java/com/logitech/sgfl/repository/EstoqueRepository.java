package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.Estoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstoqueRepository
        extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByProduto_Id(
            Long produtoId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e
            from Estoque e
            join fetch e.produto
            where e.produto.id = :produtoId
            """)
    Optional<Estoque> findByProdutoIdForUpdate(
            @Param("produtoId") Long produtoId
    );
}