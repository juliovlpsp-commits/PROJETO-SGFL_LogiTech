package com.logitech.sgfl.dto;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.function.Function;

/** Stable paginated response contract shared by list endpoints. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <S, T> PageResponse<T> from(Page<S> source, Function<S, T> mapper) {
        return new PageResponse<>(source.getContent().stream().map(mapper).toList(), source.getNumber(),
                source.getSize(), source.getTotalElements(), source.getTotalPages());
    }
}
