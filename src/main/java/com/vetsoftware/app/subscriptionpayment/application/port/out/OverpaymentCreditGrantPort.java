package com.vetsoftware.app.subscriptionpayment.application.port.out;

import java.math.BigDecimal;

public interface OverpaymentCreditGrantPort {

    /**
     * Concede saldo a favor por el exceso de {@code paymentId} sobre lo que el
     * documento podia recibir. Idempotente por {@code paymentId}+documento: un
     * reintento de la confirmacion no abre un segundo lote.
     */
    void grantForOverpayment(Long companyId, Long paymentId, Long documentId, BigDecimal amount);
}
