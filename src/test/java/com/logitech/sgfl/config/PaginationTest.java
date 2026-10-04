package com.logitech.sgfl.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationTest {
    @Test
    void normalizaNumeroDePaginaETetoDeTamanho() {
        assertThat(Pagination.request(-4, 500).getPageNumber()).isZero();
        assertThat(Pagination.request(2, 500).getPageSize()).isEqualTo(Pagination.MAX_SIZE);
        assertThat(Pagination.request(0, 0).getPageSize()).isEqualTo(1);
        assertThat(Pagination.request(0, 20).getSort().getOrderFor("id").getDirection().isDescending()).isTrue();
    }
}
