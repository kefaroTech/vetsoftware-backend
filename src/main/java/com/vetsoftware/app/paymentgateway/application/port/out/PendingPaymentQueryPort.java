package com.vetsoftware.app.paymentgateway.application.port.out;

/**
 * Si el documento tiene ya un pago {@code PENDING} aplicado, que es de otra
 * feature ({@code subscriptionpayment}). Mientras lo esté, ni el webhook ni el
 * siguiente sondeo lo han cerrado: cobrar otra vez sería un segundo cargo sobre
 * la misma tarjeta.
 */
public interface PendingPaymentQueryPort {
    boolean existsPendingPayment(Long companyId, Long billingDocumentId);
}
