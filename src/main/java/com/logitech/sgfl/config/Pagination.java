package com.logitech.sgfl.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pagination {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Pagination() {}

    public static Pageable request(int page, int size) {
        return request(page, size, Sort.by("id").descending());
    }

    public static Pageable request(int page, int size, Sort ordenacao) {
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_SIZE), ordenacao);
    }
}
