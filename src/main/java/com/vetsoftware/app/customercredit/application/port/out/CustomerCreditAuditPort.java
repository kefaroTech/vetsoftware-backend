package com.vetsoftware.app.customercredit.application.port.out;

import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import java.math.BigDecimal;

/**
 * Rastro de auditoria del saldo a favor; ver
 * {@code AuditLogger#customerCreditGranted}.
 */
public interface CustomerCreditAuditPort {

    /**
     * @param originKind
     *            de donde sale el abono; espejo de {@code chk_cce_origin_branch}
     * @param originPaymentId
     *            poblado solo cuando {@code originKind} es {@code OVERPAYMENT}
     * @param originDocumentId
     *            poblado solo cuando {@code originKind} apunta a un documento de
     *            cobro
     * @param originSubscriptionId
     *            poblado solo cuando {@code originKind} es {@code CANCELLATION}
     */
    void granted(Long entryId, Long companyId, BigDecimal amount, CreditOriginKind originKind,
            Long originPaymentId, Long originDocumentId, Long originSubscriptionId);
}
