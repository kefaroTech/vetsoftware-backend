package com.vetsoftware.app.subscriptionitemlimit.application.port.in;

import com.vetsoftware.app.subscriptionitemlimit.application.command.PropagateCatalogLimitImprovementCommand;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Propaga una mejora del cupo de fábrica a los contratos vivos.
 *
 * <p>
 * <strong>Solo mejoras.</strong> Subir el cupo de 100 a 200 llega a los
 * cuarenta contratos vivos sin crear cuarenta excepciones negociadas; bajarlo
 * de 100 a 80 no toca ninguno.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas. Cruza todas las empresas por
 * definición, así que ningún principal de tenant puede invocarla.
 *
 * @return cuántos contratos cambiaron de verdad
 */
public interface PropagateCatalogLimitImprovementUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    int execute(PropagateCatalogLimitImprovementCommand command);
}
