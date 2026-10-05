package com.logitech.sgfl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitech.sgfl.me.AuditoriaRegistro;
import com.logitech.sgfl.repository.AuditoriaRegistroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Auditoria transversal: registra criação, alteração e exclusão de cliente,
 * produto e pedido em {@code auditoria_registro}.
 *
 * As entidades são mutadas dentro da transação (dirty checking), então o
 * estado "antes" precisa ser capturado com {@link #fotografia(Object)} antes
 * das alterações e repassado para {@link #registrar}.
 */
@Service
public class AuditoriaTransversalService {

    private static final Logger log =
            LoggerFactory.getLogger(AuditoriaTransversalService.class);

    private static final int LIMITE_USUARIO = 120;

    private final AuditoriaRegistroRepository repository;
    private final ObjectMapper objectMapper;

    public AuditoriaTransversalService(
            AuditoriaRegistroRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void registrar(
            String entidade,
            Long entidadeId,
            String acao,
            String descricao,
            Object antes,
            Object depois
    ) {

        AuditoriaRegistro registro = new AuditoriaRegistro();
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setAcao(acao);
        registro.setDescricao(descricao);
        registro.setDadosAntes(resumir(antes));
        registro.setDadosDepois(resumir(depois));
        registro.setUsuario(usuarioAtual());
        registro.setCriadoEm(LocalDateTime.now());

        repository.save(registro);
    }

    @Transactional
    public void registrar(
            String entidade,
            Long entidadeId,
            String acao,
            String descricao
    ) {
        registrar(entidade, entidadeId, acao, descricao, null, null);
    }

    /**
     * Captura um resumo do estado atual de uma entidade para ser usado como
     * "antes" quando o método seguir alterando o objeto gerenciado.
     */
    public Object fotografia(Object alvo) {
        return alvo == null ? null : resumirCampos(alvo);
    }

    private String resumir(Object alvo) {

        if (alvo == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(resumirCampos(alvo));
        } catch (JsonProcessingException e) {
            log.warn("Não foi possível serializar o estado para a auditoria: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Mantém apenas campos simples (primitivos, String, número, enum e data).
     * Relações são ignoradas de propósito: serializá-las recriaria o grafo
     * inteiro (pedido -> itens -> produto) e estouraria o lazy loading.
     */
    private Object resumirCampos(Object alvo) {

        if (alvo instanceof Map<?, ?> mapa) {
            return mapa;
        }

        Map<String, Object> resumo = new LinkedHashMap<>();

        for (Class<?> tipo = alvo.getClass();
             tipo != null && tipo != Object.class;
             tipo = tipo.getSuperclass()) {

            for (Field campo : tipo.getDeclaredFields()) {

                if (Modifier.isStatic(campo.getModifiers()) || campo.isSynthetic()) {
                    continue;
                }

                if (!simples(campo.getType())) {
                    continue;
                }

                campo.setAccessible(true);

                try {
                    resumo.put(campo.getName(), normalizarValor(campo.get(alvo)));
                } catch (IllegalAccessException | RuntimeException e) {
                    log.debug("Campo ignorado na auditoria: {}", campo.getName());
                }
            }
        }

        return resumo;
    }

    private boolean simples(Class<?> tipo) {
        return tipo.isPrimitive()
                || tipo.isEnum()
                || CharSequence.class.isAssignableFrom(tipo)
                || Number.class.isAssignableFrom(tipo)
                || tipo == Boolean.class
                || Temporal.class.isAssignableFrom(tipo)
                || Date.class.isAssignableFrom(tipo);
    }

    private Object normalizarValor(Object valor) {
        if (valor instanceof Enum<?> enumerado) {
            return enumerado.name();
        }
        if (valor instanceof Temporal || valor instanceof Date) {
            return valor.toString();
        }
        return valor;
    }

    private String usuarioAtual() {

        Authentication autenticacao =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao.getName() == null
                || autenticacao.getName().isBlank()) {
            return "sistema";
        }

        String nome = autenticacao.getName().trim();

        return nome.length() > LIMITE_USUARIO
                ? nome.substring(0, LIMITE_USUARIO)
                : nome;
    }
}
