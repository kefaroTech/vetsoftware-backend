package com.vetsoftware.app.catalogitemlimit.application.port.out;

import com.vetsoftware.app.catalogitemlimit.domain.LimitDimensionRef;
import java.util.Optional;

/**
 * Resuelve el eje y su tipo de medida.
 *
 * <p>
 * No declara variante acotada por empresa porque no hay empresa que acotar: el
 * catálogo de ejes es global de plataforma.
 */
public interface LimitDimensionQueryPort {

    Optional<LimitDimensionRef> findById(Long limitDimensionId);
}
