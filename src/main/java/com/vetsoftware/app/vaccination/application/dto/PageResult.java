package com.vetsoftware.app.vaccination.application.dto;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados de un listado.
 *
 * <p>
 * Se repite por feature porque el vertical slicing prohíbe compartir DTOs de
 * aplicación entre paquetes raíz. Es duplicación deliberada.
 */
public record PageResult<T>(List<T> content, int page, int pageSize, long totalElements,
        int totalPages) {
    public <R> PageResult<R> map(Function<T, R> fn) {
        return new PageResult<>(content.stream().map(fn).toList(), page, pageSize, totalElements,
                totalPages);
    }
}
