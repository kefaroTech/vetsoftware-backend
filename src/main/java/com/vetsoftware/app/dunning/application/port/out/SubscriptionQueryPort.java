package com.vetsoftware.app.dunning.application.port.out;

import com.vetsoftware.app.dunning.domain.SubscriptionRef;
import java.util.Optional;

/**
 * Resuelve el contrato que vive en {@code subscription}.
 *
 * <p>
 * <strong>Solo la variante acotada por empresa.</strong> Con una resolucion
 * ancha, un evento de cobranza de una clinica podia colgarse del contrato de
 * otra: la fila seria de tu empresa pero el expediente apuntaria al contrato de
 * la vecina ({@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}, BE-COV).
 */
public interface SubscriptionQueryPort {
    Optional<SubscriptionRef> findByIdAndCompanyId(Long subscriptionId, Long companyId);
}
