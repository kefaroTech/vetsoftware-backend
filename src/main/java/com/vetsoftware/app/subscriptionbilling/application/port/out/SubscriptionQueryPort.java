package com.vetsoftware.app.subscriptionbilling.application.port.out;

import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionRef;
import java.util.Optional;

/**
 * Resuelve el contrato al que cuelga lo que se devenga y lo que se cobra.
 *
 * <p>
 * <b>Solo declara la variante acotada por empresa, y eso es deliberado</b>
 * ({@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}). La forma ancha
 * —{@code findById(subscriptionId)}— no se apropia de nada ajeno, pero permite
 * <b>colgar lo propio de un padre de otro tenant</b>: un cargo de la clínica A
 * facturado contra el contrato de la clínica B. Aquí ni siquiera existe el
 * método con el que equivocarse.
 */
public interface SubscriptionQueryPort {

    Optional<SubscriptionRef> findByIdAndCompanyId(Long subscriptionId, Long companyId);
}
