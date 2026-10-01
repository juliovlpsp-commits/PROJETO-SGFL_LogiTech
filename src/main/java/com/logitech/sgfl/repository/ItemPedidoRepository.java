package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemPedidoRepository
        extends JpaRepository<ItemPedido, Long> {

    boolean existsByProduto_Id(
            Long produtoId
    );
}