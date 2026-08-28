package com.vetsoftware.app.paymentreversal.application.port.out;

import com.vetsoftware.app.paymentreversal.domain.SubscriptionPaymentRef;
import java.util.Optional;

/**
 * Resuelve el pago sobre el que se instruye la reversion.
 *
 * <p>
 * <strong>Solo declara la variante acotada.</strong> Es la cuarta forma de fuga
 * de BE-COV: con la carga propia ya acotada, lo que quedaria por poder hacer es
 * <em>colgar el expediente propio del pago de otro tenant</em>, y el resultado
 * no es un rechazo sino una reversion de tu empresa apuntando al cobro de la
 * vecina. Que la variante ancha no exista es lo que lo impide
 * ({@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}).
 */
public interface SubscriptionPaymentQueryPort {

    Optional<SubscriptionPaymentRef> findByIdAndCompanyId(Long paymentId, Long companyId);
}
