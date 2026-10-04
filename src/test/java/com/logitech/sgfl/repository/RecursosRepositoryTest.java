package com.logitech.sgfl.repository;

import com.logitech.sgfl.enums.TipoCNH;
import com.logitech.sgfl.me.Caminhao;
import com.logitech.sgfl.me.Motorista;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class RecursosRepositoryTest {
    @Autowired private MotoristaRepository motoristaRepository;
    @Autowired private VeiculoRepository veiculoRepository;

    @Test
    void motoristaTemFiltroCaseInsensitiveEContagemDePagina() {
        motoristaRepository.save(new Motorista("Ana Silva", "11111111111", TipoCNH.B));
        motoristaRepository.save(new Motorista("Bruno Souza", "22222222222", TipoCNH.D));
        motoristaRepository.save(new Motorista("Ana Lima", "33333333333", TipoCNH.E));

        Page<Motorista> page = motoristaRepository.findByNomeContainingIgnoreCaseOrCpfContaining(
                "ana", "ana", PageRequest.of(0, 1, Sort.by("id").descending()));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getNome()).isEqualTo("Ana Lima");
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void veiculoFiltraPorPlacaOuModeloEAplicaOrdenacaoPaginada() {
        veiculoRepository.save(new Caminhao("AAA0001", "Volvo FH", 10_000, 4));
        veiculoRepository.save(new Caminhao("BBB0002", "Scania R", 12_000, 6));
        veiculoRepository.save(new Caminhao("CCC0003", "Volvo VM", 8_000, 3));

        Page<com.logitech.sgfl.me.Veiculo> page =
                veiculoRepository.findByPlacaContainingIgnoreCaseOrModeloContainingIgnoreCase(
                        "volvo", "volvo", PageRequest.of(0, 10, Sort.by("id").descending()));

        assertThat(page.getContent()).extracting(com.logitech.sgfl.me.Veiculo::getModelo)
                .containsExactly("Volvo VM", "Volvo FH");
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
