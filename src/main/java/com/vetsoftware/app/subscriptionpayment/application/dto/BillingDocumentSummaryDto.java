package com.vetsoftware.app.subscriptionpayment.application.dto;

import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentRef;
import java.math.BigDecimal;

public record BillingDocumentSummaryDto(Long id, Long companyId, String documentNumber,
        String documentKind, BigDecimal totalAmount, BigDecimal balanceAmount) {
    public static BillingDocumentSummaryDto from(BillingDocumentRef ref) {
        return new BillingDocumentSummaryDto(ref.id(), ref.companyId(), ref.documentNumber(),
                ref.documentKind(), ref.totalAmount(), ref.balanceAmount());
    }
}
