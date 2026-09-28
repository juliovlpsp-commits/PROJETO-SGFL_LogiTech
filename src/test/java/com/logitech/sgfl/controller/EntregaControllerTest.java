package com.logitech.sgfl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitech.sgfl.dto.EntregaRequest;
import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.service.ServicoGerenciamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa o controller isolado da cadeia de autenticação (addFilters = false):
 * o objetivo aqui é validar a lógica de negócio/validação/DTOs, não o JWT
 * (isso já é coberto por JwtServiceTest e pelo teste de integração de auth).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class EntregaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EntregaRepository entregaRepository;

    @MockBean
    private ServicoGerenciamento servicoGerenciamento;

    @Test
    void devePermitirCriarEntregaComDadosValidos() throws Exception {
        Entrega salva = new Entrega();
        // Entrega nao tem setId() publico de proposito (o id e gerado pelo banco),
        // entao usamos reflexao so aqui no teste para simular o retorno do save().
        ReflectionTestUtils.setField(salva, "id", 1L);
        salva.setDescricao("Encomenda #1092");
        salva.setEnderecoDestino("Av. Central, 500");
        salva.setStatus(StatusEntrega.PENDENTE);
        when(entregaRepository.save(any(Entrega.class))).thenReturn(salva);

        EntregaRequest request = new EntregaRequest();
        request.setDescricao("Encomenda #1092");
        request.setEnderecoDestino("Av. Central, 500");
        request.setStatus(StatusEntrega.PENDENTE);

        mockMvc.perform(post("/api/entregas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.descricao").value("Encomenda #1092"));
    }

    @Test
    void deveRejeitarCriacaoSemDescricao() throws Exception {
        EntregaRequest request = new EntregaRequest();
        request.setEnderecoDestino("Av. Central, 500");
        request.setStatus(StatusEntrega.PENDENTE);
        // descricao ausente de propósito -> @NotBlank deve barrar antes de chegar no repositório

        mockMvc.perform(post("/api/entregas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.descricao").exists());

        verify(entregaRepository, never()).save(any());
    }

    @Test
    void deveRejeitarCriacaoSemEstadoInicial() throws Exception {
        EntregaRequest request = new EntregaRequest();
        request.setDescricao("Encomenda #1092");
        request.setEnderecoDestino("Av. Central, 500");
        // status ausente de propósito

        mockMvc.perform(post("/api/entregas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarEntregasPaginadas() throws Exception {
        Entrega entrega = new Entrega();
        ReflectionTestUtils.setField(entrega, "id", 1L);
        Page<Entrega> pagina = new PageImpl<>(List.of(entrega), PageRequest.of(0, 10), 1);
        when(entregaRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(pagina);

        mockMvc.perform(get("/api/entregas?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deveRetornar404AoAtualizarStatusDeEntregaInexistente() throws Exception {
        when(entregaRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/entregas/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENTREGUE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRejeitarValorDeStatusQueNaoExisteNoEnum() throws Exception {
        mockMvc.perform(patch("/api/entregas/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VOANDO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveExcluirEntregaExistente() throws Exception {
        when(entregaRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/entregas/1"))
                .andExpect(status().isNoContent());

        verify(entregaRepository).deleteById(1L);
    }

    @Test
    void deveRetornar404AoExcluirEntregaInexistente() throws Exception {
        when(entregaRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/entregas/99"))
                .andExpect(status().isNotFound());

        verify(entregaRepository, never()).deleteById(anyLong());
    }
}