package com.logitech.sgfl.service;

import com.logitech.sgfl.me.Entrega;

import java.util.List;

public interface ServicoGerenciamento {

    Entrega alocarEntrega(Long entregaId, Long veiculoId, Long motoristaId);

    Entrega finalizarEntrega(Long entregaId);

    List<Entrega> listarTodas();
}