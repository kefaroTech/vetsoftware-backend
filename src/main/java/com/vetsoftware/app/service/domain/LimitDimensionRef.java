package com.vetsoftware.app.service.domain;

/**
 * El eje de límite tal como lo necesita esta rodaja: su identificador y su
 * código, nada más. Companion VO propio, no la entidad de dominio de
 * {@code limitdimension}. Lo resuelve {@code LimitDimensionQueryPort} en una
 * sola consulta.
 */
public record LimitDimensionRef(Long id, String code) {

    public LimitDimensionRef {
        if (id == null) {
            throw new IllegalArgumentException("limit dimension id is required");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("limit dimension code is required");
        }
    }
}
