package com.vetsoftware.app.quote.application.port.out;

/**
 * Marca el medio de pago como predeterminado de la empresa, si aún no lo era.
 * Es otra feature ({@code subscriptionpaymentmethod}) la que decide qué
 * significa "predeterminado" y libera el hueco del anterior.
 */
public interface PaymentSourceDefaultingPort {

    void markDefaultIfNeeded(Long companyId, Long paymentSourceId);
}
