package com.vetsoftware.app.catalogitem.application.port.out;

import com.vetsoftware.app.catalogitem.domain.SubModuleRef;
import java.util.Optional;

/**
 * Resuelve el submódulo que abre un artículo sin que nada fuera de
 * {@code infrastructure/persistence} conozca la feature {@code submodule}.
 *
 * <p>
 * No declara variante acotada por empresa porque no hay empresa que acotar:
 * {@code sub_modules} es un catálogo global de plataforma (choque C7 de la
 * especificación) y {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} solo
 * aplica cuando la entidad referida pertenece a una empresa.
 */
public interface SubModuleQueryPort {

    Optional<SubModuleRef> findById(Long subModuleId);
}
