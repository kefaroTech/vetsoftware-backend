package com.vetsoftware.app.clinicalhistory.application.dto;

import java.util.List;
import java.util.function.Function;

/**
 * Pagina de resultados de la feature. Se duplica por feature a proposito: el
 * vertical slicing prohibe compartir DTOs entre paquetes raiz.
 */
public record PageResult<T>(List<T> content, int page, int pageSize, long totalElements,
        int totalPages) {
    public <R> PageResult<R> map(Function<T, R> fn) {
        return new PageResult<>(content.stream().map(fn).toList(), page, pageSize, totalElements,
                totalPages);
    }
}
