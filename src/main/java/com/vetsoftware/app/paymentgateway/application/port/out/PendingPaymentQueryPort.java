package com.vetsoftware.app.paymentgateway.application.port.out;

/**
 * Si el documento tiene ya un pago {@code PENDING} aplicado, que es de otra
 * feature ({@code subscriptionpayment}). Mientras lo esté, ni el webhook ni el
 * siguiente sondeo lo han cerrado: cobrar otra vez sería un segundo cargo sobre
 * la misma tarjeta.
 *
 * <p>
 * Excepción: un {@code PENDING} con un intento de reintento anotado después de
 * su recepción ya no cuenta como bloqueante. Es la marca que deja
 * {@code ReconcilePendingPaymentsService} cuando Wompi lo sostiene más allá del
 * umbral de antigüedad sin darlo por fallido —para no cerrarle la puerta a una
 * aprobación tardía—, así que el documento vuelve a ser cobrable aunque esa
 * fila siga {@code PENDING}.
 */
public interface PendingPaymentQueryPort {
    boolean existsPendingPayment(Long companyId, Long billingDocumentId);
}
