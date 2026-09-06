package com.vetsoftware.app.paymentgateway.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * El lado de tesorería del cobro, delegando en {@code subscriptionpayment}:
 * registrar el pago, aplicarlo a la factura, y confirmarlo o marcarlo fallido
 * cuando la pasarela resuelve el estado final.
 */
public interface SubscriptionPaymentLedgerPort {

    /**
     * Registra el pago (nace {@code PENDING}) y lo aplica al documento. Devuelve el
     * id del pago.
     */
    Long registerAndApply(Long companyId, BigDecimal amount, String currency,
            String gatewayReference, LocalDateTime receivedAt, String clientRequestId,
            Long documentId);

    void confirm(Long paymentId, Long companyId);

    void fail(Long paymentId, Long companyId);

    /**
     * La factura a la que se aplicó este pago, para poder anotar el intento
     * fallido.
     */
    Optional<Long> findDocumentIdByPayment(Long companyId, Long paymentId);
}
