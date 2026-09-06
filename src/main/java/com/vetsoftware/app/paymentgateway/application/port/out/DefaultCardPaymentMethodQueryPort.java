package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import java.util.Optional;

/**
 * El medio de pago predeterminado y vigente de una empresa para un gateway
 * dado, que es de otra feature ({@code subscriptionpaymentmethod}).
 */
public interface DefaultCardPaymentMethodQueryPort {
    Optional<PaymentMethodRef> findDefaultActiveCard(Long companyId, String gateway);
}
