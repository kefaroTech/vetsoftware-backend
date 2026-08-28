package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.domain.LimitDimensionRef;
import java.util.Optional;

/**
 * Resuelve el eje por su codigo, que es lo que guarda
 * {@code subscription_items.capacity_unit}.
 *
 * <p>
 * <strong>Existe por una sola razon, y es la puerta de entrada.</strong> Las
 * lineas que se firman desde la consola llegan por HTTP con la unidad escrita
 * en el cuerpo. Mientras la columna admitia cuatro literales, Jackson rechazaba
 * cualquier otra cosa al deserializar el enumerado; ahora que admite el
 * catalogo entero, lo unico que separa un {@code 400} legible de un {@code 500}
 * con violacion de clave foranea es esta consulta.
 *
 * <p>
 * No declara variante acotada por empresa: el catalogo de ejes es global de
 * plataforma.
 */
public interface LimitDimensionQueryPort {

    Optional<LimitDimensionRef> findByCode(String code);
}
