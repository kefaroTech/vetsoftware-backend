package com.vetsoftware.app.cashregister.application.dto;

import java.util.List;
import java.util.function.Function;

/** Página de resultados (propia de la feature; no se comparte entre features por el vertical slicing). */
public record PageResult<T>(
        List<T> content,
        int page,
        int pageSize,
        long totalElements,
        int totalPages
) {
    public <R> PageResult<R> map(Function<T, R> fn) {
        return new PageResult<>(content.stream().map(fn).toList(), page, pageSize, totalElements, totalPages);
    }
}
