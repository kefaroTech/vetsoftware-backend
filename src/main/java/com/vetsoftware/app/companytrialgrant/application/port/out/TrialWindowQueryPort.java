package com.vetsoftware.app.companytrialgrant.application.port.out;

import com.vetsoftware.app.companytrialgrant.domain.TrialWindowRef;
import java.util.Optional;

/**
 * Resuelve la ventana de la empresa sin importar el dominio de
 * {@code companytrialwindow}.
 *
 * <p>
 * <strong>Solo declara la variante acotada por empresa.</strong> Una ventana
 * pertenece a alguien: resolverla por un id suelto permitiría colgar la prueba
 * de una clínica de la ventana de otra y heredar un techo ajeno, que es el
 * defecto que la clave foránea triple existe para impedir. Aquí no hay
 * {@code findById(Long)} y no debe haberlo.
 */
public interface TrialWindowQueryPort {

    Optional<TrialWindowRef> findOpenByCompanyId(Long companyId);

    Optional<TrialWindowRef> findByIdAndCompanyId(Long trialWindowId, Long companyId);
}
