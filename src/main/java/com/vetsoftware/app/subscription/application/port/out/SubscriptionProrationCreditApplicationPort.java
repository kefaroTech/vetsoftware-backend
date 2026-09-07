package com.vetsoftware.app.subscription.application.port.out;

import java.math.BigDecimal;

/**
 * Aplica el saldo a favor por prorrateo del contrato anterior contra el
 * documento del primer periodo del contrato nuevo, en la misma transaccion de
 * la firma.
 */
public interface SubscriptionProrationCreditApplicationPort {

    void applyToDocument(Long companyId, Long documentId, Long creditEntryId, BigDecimal amount,
            String clientRequestId);
}
