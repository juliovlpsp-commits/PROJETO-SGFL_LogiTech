package com.logitech.sgfl.repository;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.me.Motorista;
import com.logitech.sgfl.me.Veiculo;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EntregaSpecifications {

    private EntregaSpecifications() {}

    public static Specification<Entrega> filtrar(StatusEntrega status, String termo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> filtros = new ArrayList<>();

            if (status != null) {
                filtros.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (termo != null && !termo.isBlank()) {
                String termoNormalizado = termo.trim().toLowerCase(Locale.ROOT);
                String padrao = "%" + escaparLike(termoNormalizado) + "%";
                List<Predicate> correspondencias = new ArrayList<>();

                adicionarTexto(criteriaBuilder, correspondencias, root.<String>get("descricao"), padrao);
                adicionarTexto(criteriaBuilder, correspondencias, root.<String>get("enderecoOrigem"), padrao);
                adicionarTexto(criteriaBuilder, correspondencias, root.<String>get("enderecoDestino"), padrao);

                Join<Entrega, Motorista> motorista = root.join("motorista", JoinType.LEFT);
                adicionarTexto(criteriaBuilder, correspondencias, motorista.<String>get("nome"), padrao);

                Join<Entrega, Veiculo> veiculo = root.join("veiculo", JoinType.LEFT);
                adicionarTexto(criteriaBuilder, correspondencias, veiculo.<String>get("placa"), padrao);
                adicionarTexto(criteriaBuilder, correspondencias, veiculo.<String>get("modelo"), padrao);

                String idTermo = termoNormalizado.startsWith("#")
                        ? termoNormalizado.substring(1).trim()
                        : termoNormalizado;
                if (idTermo.matches("\\d+")) {
                    try {
                        correspondencias.add(criteriaBuilder.equal(root.get("id"), Long.parseLong(idTermo)));
                    } catch (NumberFormatException ignored) {
                        // Um número maior que Long ainda pode corresponder aos campos textuais.
                    }
                }

                filtros.add(criteriaBuilder.or(correspondencias.toArray(Predicate[]::new)));
            }

            return criteriaBuilder.and(filtros.toArray(Predicate[]::new));
        };
    }

    private static void adicionarTexto(
            CriteriaBuilder criteriaBuilder,
            List<Predicate> correspondencias,
            Expression<String> campo,
            String padrao
    ) {
        correspondencias.add(criteriaBuilder.like(criteriaBuilder.lower(campo), padrao, '\\'));
    }

    private static String escaparLike(String termo) {
        return termo.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
