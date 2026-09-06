package com.vetsoftware.app.paymentgateway.application.port.out;

import java.time.LocalDateTime;

/**
 * Reprograma un intento vencido, delegando en {@code paymentattempt}. Solo hace
 * falta cuando el cobro <strong>no</strong> llegó a registrar un intento nuevo:
 * uno nuevo ya trae su propio {@code nextAttemptAt} y el viejo queda en el
 * historial.
 */
public interface PaymentAttemptReschedulerPort {
    void reschedule(Long attemptId, Long companyId, LocalDateTime nextAttemptAt);
}
