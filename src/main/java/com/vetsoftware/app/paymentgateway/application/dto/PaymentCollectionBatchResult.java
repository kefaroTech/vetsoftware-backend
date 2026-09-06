package com.vetsoftware.app.paymentgateway.application.dto;

import com.vetsoftware.app.paymentgateway.domain.DocumentChargeOutcome;
import java.util.Map;

/**
 * @param lastId
 *            solo tiene sentido en {@code collectNewChargesAfter}: el cursor de
 *            la siguiente vuelta. {@code collectDueRetries} pagina por número
 *            de página, no por id, y lo deja en 0
 */
public record PaymentCollectionBatchResult(int processed, int failures, long lastId,
        Map<DocumentChargeOutcome, Integer> outcomeCounts) {

    public PaymentCollectionBatchResult {
        outcomeCounts = Map.copyOf(outcomeCounts);
    }
}
