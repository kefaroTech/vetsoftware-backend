package com.vetsoftware.app.limitdimension.application.port.out;

import com.vetsoftware.app.limitdimension.domain.SubModuleRef;
import java.util.Optional;

/**
 * Resuelve el submódulo del que cuelga un eje sin que nada fuera de
 * {@code infrastructure/persistence} conozca la feature {@code submodule}.
 *
 * <p>
 * No declara variante acotada por empresa porque no hay empresa que acotar:
 * {@code sub_modules} es catálogo global de plataforma.
 */
public interface SubModuleQueryPort {

    Optional<SubModuleRef> findById(Long subModuleId);
}
