package com.vetsoftware.app.subscription.domain;

/**
 * El eje visto desde el contrato. Companion VO: el dominio de
 * {@code limitdimension} no se importa.
 *
 * <p>
 * Sin {@code measureKind}, por lo mismo que su gemelo de {@code catalogitem}:
 * {@code subscription_items} guarda <em>que</em> eje se contrato, no
 * <em>como</em> se cuenta. El tipo de medida lo copian —y lo atan por clave
 * compuesta— las dos tablas que declaran periodo de reinicio o excedente
 * ({@code catalog_item_limits} y {@code company_capacities}); una linea de
 * contrato no declara ninguna de las dos cosas.
 *
 * <p>
 * Se identifica por {@code code} porque es lo que guarda la columna:
 * {@code capacity_unit} <strong>es</strong> el codigo del eje desde el
 * changeset 333, y la clave foranea apunta a {@code limit_dimensions(code)}.
 */
public record LimitDimensionRef(Long id, String code) {

    public LimitDimensionRef {
        if (id == null)
            throw new IllegalArgumentException("limit dimension id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("limit dimension code is required");
    }
}
