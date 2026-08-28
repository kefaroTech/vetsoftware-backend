package com.vetsoftware.app.catalogitemlimit.domain;

/**
 * El eje visto desde el techo de fábrica. Companion VO: el dominio de
 * {@code limitdimension} no se importa.
 *
 * <p>
 * Trae el {@code measureKind} porque de él dependen tres invariantes que la
 * fila tiene que poder comprobar sola —el periodo de reinicio, la prohibición
 * del excedente sobre acumulativos y el enfriamiento—, y una restricción no
 * puede mirar otra tabla. Por eso el valor se copia en la columna y esa copia
 * va atada por clave foránea al eje real.
 */
public record LimitDimensionRef(Long id, String code, MeasureKind measureKind) {

    public LimitDimensionRef {
        if (id == null)
            throw new IllegalArgumentException("limit dimension id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("limit dimension code is required");
        if (measureKind == null)
            throw new IllegalArgumentException("limit dimension measure kind is required");
    }
}
