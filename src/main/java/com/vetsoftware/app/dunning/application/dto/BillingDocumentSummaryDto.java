package com.vetsoftware.app.dunning.application.dto;

import com.vetsoftware.app.dunning.domain.BillingDocumentRef;
import java.math.BigDecimal;

public record BillingDocumentSummaryDto(Long id, Long companyId, String documentNumber,
        BigDecimal balanceAmount) {
    public static BillingDocumentSummaryDto from(BillingDocumentRef ref) {
        return new BillingDocumentSummaryDto(ref.id(), ref.companyId(), ref.documentNumber(),
                ref.balanceAmount());
    }
}
