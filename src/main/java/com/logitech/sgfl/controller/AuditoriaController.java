package com.logitech.sgfl.controller;

import com.logitech.sgfl.config.Pagination;
import com.logitech.sgfl.dto.AuditoriaRegistroResponse;
import com.logitech.sgfl.dto.PageResponse;
import com.logitech.sgfl.me.AuditoriaRegistro;
import com.logitech.sgfl.repository.AuditoriaRegistroRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Consulta da auditoria transversal (cliente, produto e pedido).
 * Restrita a ADMIN no SecurityConfig.
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaRegistroRepository repository;

    public AuditoriaController(
            AuditoriaRegistroRepository repository
    ) {
        this.repository = repository;
    }

    @GetMapping
    public PageResponse<AuditoriaRegistroResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) Long entidadeId,
            @RequestParam(required = false) String acao
    ) {

        Specification<AuditoriaRegistro> filtros =
                (root, query, criteriaBuilder) -> {

                    List<Predicate> criterios = new ArrayList<>();

                    if (entidade != null && !entidade.isBlank()) {
                        criterios.add(
                                criteriaBuilder.equal(
                                        criteriaBuilder.upper(root.get("entidade")),
                                        entidade.trim().toUpperCase(Locale.ROOT)
                                )
                        );
                    }

                    if (entidadeId != null) {
                        criterios.add(
                                criteriaBuilder.equal(
                                        root.get("entidadeId"),
                                        entidadeId
                                )
                        );
                    }

                    if (acao != null && !acao.isBlank()) {
                        criterios.add(
                                criteriaBuilder.equal(
                                        criteriaBuilder.upper(root.get("acao")),
                                        acao.trim().toUpperCase(Locale.ROOT)
                                )
                        );
                    }

                    return criteriaBuilder.and(
                            criterios.toArray(new Predicate[0])
                    );
                };

        Page<AuditoriaRegistro> registros = repository.findAll(
                filtros,
                Pagination.request(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "criadoEm")
                )
        );

        return PageResponse.from(
                registros,
                AuditoriaRegistroResponse::from
        );
    }
}
