package com.vetsoftware.app.paymentattempt.application.port.out;

/**
 * La FK compuesta {@code (company_id, payment_method_id)} a
 * {@code subscription_payment_methods}, que es de otra feature.
 *
 * <p>
 * {@code ValidationPort} por el mismo motivo que
 * {@link BillingDocumentValidationPort}: el intento no lee la marca ni los
 * cuatro ultimos digitos de la tarjeta, y <strong>no debe leer nunca el
 * testigo</strong> de la pasarela. Solo necesita saber que el medio existe y es
 * de esa empresa.
 *
 * <p>
 * Acotado por empresa: un medio de pago pertenece a una clinica, asi que la
 * variante ancha dejaria cobrar con la tarjeta de otra.
 */
public interface SubscriptionPaymentMethodValidationPort {

    /** {@code true} si el medio de pago existe y pertenece a esa empresa. */
    boolean existsByIdAndCompanyId(Long paymentMethodId, Long companyId);
}
