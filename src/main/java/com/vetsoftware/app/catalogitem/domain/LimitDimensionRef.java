package com.vetsoftware.app.catalogitem.domain;

/**
 * El eje visto desde el estante de la tienda. Companion VO: el dominio de
 * {@code limitdimension} no se importa.
 *
 * <p>
 * <strong>Es mas pequeño que su gemelo de {@code catalogitemlimit} a
 * proposito.</strong> Aquel trae {@code measureKind} porque de el dependen tres
 * invariantes que su fila tiene que poder comprobar sola —el periodo de
 * reinicio, la prohibicion del excedente sobre acumulativos y el enfriamiento—,
 * y por eso el tipo de medida se copia a su columna y va atado por clave
 * foranea compuesta. {@code catalog_items} no declara ninguna de esas tres
 * cosas: guarda <em>que</em> eje se vende, no <em>como</em> se cuenta. Un campo
 * que no se usa no es informacion de mas, es un valor que alguien acabara
 * leyendo de la fila equivocada.
 *
 * <p>
 * Se identifica por {@code code} y no por {@code id} porque la columna que lo
 * referencia guarda el codigo: {@code capacity_unit} <strong>es</strong> el
 * codigo del eje desde el changeset 333, y la clave foranea apunta a
 * {@code limit_dimensions(code)}.
 */
public record LimitDimensionRef(Long id, String code) {

    public LimitDimensionRef {
        if (id == null)
            throw new IllegalArgumentException("limit dimension id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("limit dimension code is required");
    }
}
