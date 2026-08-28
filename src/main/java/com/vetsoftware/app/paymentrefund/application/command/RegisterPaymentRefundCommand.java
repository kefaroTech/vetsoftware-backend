package com.vetsoftware.app.paymentrefund.application.command;

import com.vetsoftware.app.paymentrefund.domain.RefundMethod;
import com.vetsoftware.app.paymentrefund.domain.RefundReasonCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param companyId
 *            la empresa a la que se le devuelve. Viaja en el cuerpo porque este
 *            caso de uso es de plataforma y solo de plataforma: quien lo invoca
 *            es tesoreria, que si elige a que clinica le devuelve. En el camino
 *            de tenant esto seria la fuga que {@code CLAUDE.md} prohibe, pero
 *            ese camino no existe aqui
 * @param authorizedBySystemUserId
 *            quien firma la salida de dinero. Obligatorio: sin autorizante, la
 *            devolucion no se puede defender ante nadie
 * @param clientRequestId
 *            llave de idempotencia (R13). Toda peticion que mueve dinero la
 *            lleva, y devolver mueve dinero; sin ella un doble clic devuelve
 *            dos veces
 */
public record RegisterPaymentRefundCommand(Long companyId, Long paymentId, Long sourceDocumentId,
        BigDecimal amount, RefundMethod method, String destinationReference,
        LocalDateTime refundedAt, LocalDate valueDate, RefundReasonCode reasonCode, String reason,
        Long authorizedBySystemUserId, String clientRequestId) {
}
