package com.logitech.sgfl.service;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;

import java.util.List;

public interface ServicoGerenciamento {

    Entrega alocarEntrega(Long entregaId, Long veiculoId, Long motoristaId);

    Entrega finalizarEntrega(Long entregaId);

    Entrega atualizarStatus(Long entregaId, StatusEntrega novoStatus);

    List<Entrega> listarTodas();
}