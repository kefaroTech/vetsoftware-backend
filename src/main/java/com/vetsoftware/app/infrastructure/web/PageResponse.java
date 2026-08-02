package com.vetsoftware.app.infrastructure.web;

import java.util.List;

/** Envoltura genérica de respuesta paginada para endpoints de listado. */
public record PageResponse<T>(List<T> content, int page, int pageSize, long totalElements,
        int totalPages) {
}
