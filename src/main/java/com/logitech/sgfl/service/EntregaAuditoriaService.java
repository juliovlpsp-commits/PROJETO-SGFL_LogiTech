package com.logitech.sgfl.service;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.me.EntregaEvento;
import com.logitech.sgfl.repository.EntregaEventoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EntregaAuditoriaService {
    private final EntregaEventoRepository repository;
    private final NotificacaoEmailService notificacao;
    private final NotificacaoWhatsappService whatsapp;

    public EntregaAuditoriaService(
            EntregaEventoRepository repository,
            NotificacaoEmailService notificacao,
            NotificacaoWhatsappService whatsapp
    ) {
        this.repository = repository;
        this.notificacao = notificacao;
        this.whatsapp = whatsapp;
    }

    @Transactional
    public void registrar(Entrega entrega, String tipo, StatusEntrega anterior,
                          StatusEntrega novo, String observacao) {

        String responsavel = responsavelAtual();

        repository.save(new EntregaEvento(
                entrega,
                tipo,
                anterior,
                novo,
                LocalDateTime.now(),
                responsavel,
                observacao
        ));

        /*
         * Avisa por e-mail e WhatsApp só quando o status realmente mudou:
         * CRIADA (anterior nulo) e COMPROVANTE_ANEXADO (mesmo status)
         * não geram aviso.
         */
        if (anterior != null && novo != null && anterior != novo) {

            if (notificacao != null) {
                notificacao.notificarMudancaDeStatus(
                        entrega,
                        anterior,
                        novo,
                        responsavel
                );
            }

            if (whatsapp != null) {
                whatsapp.notificarMudancaDeStatus(
                        entrega,
                        anterior,
                        novo,
                        responsavel
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public List<EntregaEvento> timeline(Long entregaId) {
        return repository.findByEntrega_IdOrderByOcorridoEmAsc(entregaId);
    }

    private String responsavelAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "sistema";
        }
        String nome = authentication.getName();
        return nome == null || nome.isBlank() ? "sistema" : nome;
    }
}
