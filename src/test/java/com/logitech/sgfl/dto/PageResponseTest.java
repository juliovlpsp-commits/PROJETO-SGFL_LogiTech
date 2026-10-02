package com.logitech.sgfl.dto;

import com.logitech.sgfl.config.Pagination;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {
    @Test
    void mapeiaConteudoEMetadadosDaPagina() {
        var source = new PageImpl<>(List.of("cliente 3", "cliente 4"), PageRequest.of(1, 2), 5);

        var response = PageResponse.from(source, String::toUpperCase);

        assertThat(response.content()).containsExactly("CLIENTE 3", "CLIENTE 4");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void limitaTamanhoEIndiceDaPagina() {
        var pageable = Pagination.request(-3, Integer.MAX_VALUE);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(Pagination.MAX_SIZE);
    }
}
