package com.vetsoftware.app.service.application.dto;

import java.util.List;
import java.util.function.Function;

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
