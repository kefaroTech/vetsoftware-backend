package com.vetsoftware.app.catalogitem.application.port.out;

import com.vetsoftware.app.catalogitem.domain.LimitDimensionRef;
import java.util.Optional;

/**
 * Resuelve el eje por su codigo, que es lo que guarda
 * {@code catalog_items.capacity_unit}.
 *
 * <p>
 * Busca por codigo y no por id —al reves que su gemelo de
 * {@code catalogitemlimit}— porque es la columna la que manda: el articulo
 * referencia el eje por su codigo, asi que preguntar por id obligaria a
 * traducir antes en algun sitio, y ese sitio seria una traduccion mas donde
 * equivocarse.
 *
 * <p>
 * No declara variante acotada por empresa porque no hay empresa que acotar: ni
 * el catalogo de ejes ni el catalogo comercial pertenecen a ninguna.
 */
public interface LimitDimensionQueryPort {

    Optional<LimitDimensionRef> findByCode(String code);
}
