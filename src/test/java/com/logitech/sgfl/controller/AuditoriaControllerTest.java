package com.logitech.sgfl.controller;

import com.logitech.sgfl.config.Pagination;
import com.logitech.sgfl.dto.AuditoriaRegistroResponse;
import com.logitech.sgfl.dto.PageResponse;
import com.logitech.sgfl.me.AuditoriaRegistro;
import com.logitech.sgfl.repository.AuditoriaRegistroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaControllerTest {

    @Mock
    private AuditoriaRegistroRepository repository;

    @InjectMocks
    private AuditoriaController controller;

    @Test
    void deveListarRegistrosNoFormatoDoContratoDeListagem() {

        AuditoriaRegistro registro = new AuditoriaRegistro();
        registro.setEntidade("CLIENTE");
        registro.setEntidadeId(7L);
        registro.setAcao("CRIADO");
        registro.setUsuario("admin@gmail.com");
        registro.setCriadoEm(LocalDateTime.of(2026, 1, 1, 10, 0));

        when(repository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(List.of(registro))
        );

        PageResponse<AuditoriaRegistroResponse> resposta =
                controller.listar(0, 20, "cliente", 7L, "criado");

        assertThat(resposta.totalElements()).isEqualTo(1);
        assertThat(resposta.content()).hasSize(1);
        assertThat(resposta.content().get(0).entidade()).isEqualTo("CLIENTE");
        assertThat(resposta.content().get(0).usuario()).isEqualTo("admin@gmail.com");

        verify(repository).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    @Test
    void deveListarSemFiltrosERespeitarOLimiteDaPagina() {

        when(repository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenAnswer(
                invocacao -> {
                    Pageable solicitado = invocacao.getArgument(1);
                    return new PageImpl<>(List.of(), solicitado, 0);
                }
        );

        PageResponse<AuditoriaRegistroResponse> resposta =
                controller.listar(1, 500, null, null, null);

        assertThat(resposta.content()).isEmpty();
        assertThat(resposta.totalElements()).isZero();
        assertThat(resposta.page()).isEqualTo(1);
        assertThat(resposta.size()).isEqualTo(Pagination.MAX_SIZE);
    }

    @Test
    void deveSerializarOsEstadosAntesEDepois() {

        AuditoriaRegistro registro = new AuditoriaRegistro();
        registro.setEntidade("PEDIDO");
        registro.setEntidadeId(3L);
        registro.setAcao("ATUALIZADO");
        registro.setDadosAntes("{\"status\":\"ABERTO\"}");
        registro.setDadosDepois("{\"status\":\"CANCELADO\"}");
        registro.setCriadoEm(LocalDateTime.of(2026, 1, 1, 10, 0));

        AuditoriaRegistroResponse response =
                AuditoriaRegistroResponse.from(registro);

        assertThat(response.dadosAntes()).isEqualTo("{\"status\":\"ABERTO\"}");
        assertThat(response.dadosDepois()).isEqualTo("{\"status\":\"CANCELADO\"}");
        assertThat(response.acao()).isEqualTo("ATUALIZADO");
    }
}
