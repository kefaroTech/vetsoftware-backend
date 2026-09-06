package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import java.util.Optional;

/**
 * El pago del primer periodo, que es de otra feature
 * ({@code subscriptionpayment}). Sirve tanto a la idempotencia de
 * {@code ChargeContractFirstPeriodService} como a
 * {@code FindFirstPeriodPaymentUseCase}.
 */
public interface FirstPeriodPaymentQueryPort {
    Optional<FirstPeriodPaymentSnapshot> findByCompanyIdAndReference(Long companyId,
            String reference);

    /**
     * Búsqueda <strong>global</strong>, sin empresa: el webhook de Wompi no trae
     * ninguna. {@code (gateway, gatewayReference)} es único en
     * {@code subscription_payments}, así que localiza como mucho un pago.
     */
    Optional<FirstPeriodPaymentSnapshot> findByGatewayAndReference(String gateway,
            String gatewayReference);
}
