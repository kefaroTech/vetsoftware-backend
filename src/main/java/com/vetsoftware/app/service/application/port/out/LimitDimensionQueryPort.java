package com.vetsoftware.app.service.application.port.out;

import com.vetsoftware.app.service.domain.LimitDimensionRef;
import java.util.Optional;

/**
 * Resuelve el eje de límite contra el catálogo. Es un catálogo global, así que
 * no hay variante acotada por empresa que declarar.
 */
public interface LimitDimensionQueryPort {

    Optional<LimitDimensionRef> findByCode(String code);
}
