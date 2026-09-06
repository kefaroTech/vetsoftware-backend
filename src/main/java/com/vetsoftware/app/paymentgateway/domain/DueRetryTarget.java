package com.vetsoftware.app.paymentgateway.domain;

/**
 * Un intento vencido de la cola de reintentos, que es de otra feature
 * ({@code paymentattempt}), visto desde aquí.
 */
public record DueRetryTarget(Long attemptId, Long companyId, Long billingDocumentId) {
}
