package com.vetsoftware.app.subscriptionpayment.application.command;

import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @param clientRequestId
 *            llave de idempotencia del operador (R13). Cubre el doble clic de
 *            quien registra un pago manual; el reintento de la pasarela lo
 *            cubre el par {@code (gateway, gatewayReference)}
 */
public record RegisterSubscriptionPaymentCommand(Long companyId, BigDecimal amount, String currency,
        PaymentMethod paymentMethod, String gateway, String gatewayReference,
        LocalDateTime receivedAt, String clientRequestId) {
}
