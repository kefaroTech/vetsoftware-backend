package com.vetsoftware.app.subscriptionpayment.application.dto;

import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillingDocumentApplicationDto(Long id, Long companyId,
        BillingDocumentSummaryDto targetDocument, ApplicationSourceKind sourceKind, Long paymentId,
        BillingDocumentSummaryDto sourceDocument, BigDecimal appliedAmount, Long reversalOfId,
        LocalDateTime appliedAt, LocalDateTime createdDate) {

    public static BillingDocumentApplicationDto from(BillingDocumentApplication application) {
        return new BillingDocumentApplicationDto(application.getId(), application.getCompanyId(),
                BillingDocumentSummaryDto.from(application.getTargetDocument()),
                application.getSourceKind(), application.getPaymentId(),
                application.getSourceDocument() == null
                        ? null
                        : BillingDocumentSummaryDto.from(application.getSourceDocument()),
                application.getAppliedAmount(), application.getReversalOfId(),
                application.getAppliedAt(), application.getCreatedDate());
    }
}
