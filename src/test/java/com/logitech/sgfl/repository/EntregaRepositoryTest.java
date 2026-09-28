package com.logitech.sgfl.repository;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o bug relatado: depois de atualizar o status de uma entrega, a lista
 * não pode "pular" de posição na tela. A causa raiz era confiar na ordem
 * física do banco (que o Postgres pode reorganizar após um UPDATE via MVCC);
 * a correção é sempre pedir ORDER BY id explicitamente.
 */
@DataJpaTest
@ActiveProfiles("test")
class EntregaRepositoryTest {

    @Autowired
    private EntregaRepository entregaRepository;

    @Test
    void devePermanecerOrdenadaPorIdMesmoAposAtualizarUmaEntregaNoMeio() {
        Entrega primeira = entregaRepository.save(new Entrega("Origem A", "Destino A", 10));
        Entrega segunda = entregaRepository.save(new Entrega("Origem B", "Destino B", 20));
        Entrega terceira = entregaRepository.save(new Entrega("Origem C", "Destino C", 30));

        // Simula o usuário mudando o status da entrega do meio
        segunda.setStatus(StatusEntrega.ENTREGUE);
        entregaRepository.save(segunda);

        Page<Entrega> pagina = entregaRepository.findAll(PageRequest.of(0, 10, Sort.by("id").ascending()));

        assertThat(pagina.getContent())
                .extracting(Entrega::getId)
                .containsExactly(primeira.getId(), segunda.getId(), terceira.getId());
    }

    @Test
    void deveRespeitarOTamanhoDaPaginaSolicitado() {
        for (int i = 0; i < 25; i++) {
            entregaRepository.save(new Entrega("Origem " + i, "Destino " + i, i));
        }

        Page<Entrega> pagina = entregaRepository.findAll(PageRequest.of(0, 10, Sort.by("id").ascending()));

        assertThat(pagina.getContent()).hasSize(10);
        assertThat(pagina.getTotalElements()).isEqualTo(25);
        assertThat(pagina.getTotalPages()).isEqualTo(3);
    }
}
