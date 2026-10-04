package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoRepository
        extends JpaRepository<Produto, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(
            String codigo,
            Long id
    );

    @Override
    @EntityGraph(attributePaths = "estoque")
    Page<Produto> findAll(Pageable pageable);
}
