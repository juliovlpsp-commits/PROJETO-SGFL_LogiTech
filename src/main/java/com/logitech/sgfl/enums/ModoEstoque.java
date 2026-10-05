package com.logitech.sgfl.enums;

import java.util.Locale;

/**
 * Modo de baixa de estoque dos pedidos (SGFL_STOCK_MODE).
 *
 * <ul>
 *   <li>{@code IMEDIATA} (padrão): a criação do pedido já dá baixa física
 *       no estoque; cancelar devolve a quantidade.</li>
 *   <li>{@code RESERVA}: a criação apenas bloqueia (reserva) a quantidade;
 *       a baixa física acontece na conclusão do pedido (despacho/envio) e
 *       cancelar libera a reserva.</li>
 * </ul>
 *
 * Valor desconhecido ou vazio cai no padrão IMEDIATA, para nunca
 * derrubar a aplicação por causa de configuração errada.
 */
public enum ModoEstoque {

    IMEDIATA,
    RESERVA;

    public static ModoEstoque de(String valor) {
        if (valor == null || valor.isBlank()) {
            return IMEDIATA;
        }

        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return IMEDIATA;
        }
    }
}
