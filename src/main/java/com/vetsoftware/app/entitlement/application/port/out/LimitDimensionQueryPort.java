package com.vetsoftware.app.entitlement.application.port.out;

import com.vetsoftware.app.entitlement.domain.LimitDimensionRef;
import java.util.Optional;

/**
 * Resuelve el eje del catalogo y su tipo de medida.
 *
 * <p>
 * No declara variante acotada por empresa porque no hay empresa que acotar: el
 * catalogo de ejes es global de plataforma y no alcanza la tabla de empresas.
 */
public interface LimitDimensionQueryPort {

    Optional<LimitDimensionRef> findByCode(String code);
}
