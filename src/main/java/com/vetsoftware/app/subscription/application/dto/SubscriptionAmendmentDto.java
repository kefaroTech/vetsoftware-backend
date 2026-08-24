package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.AmendmentType;
import com.vetsoftware.app.subscription.domain.SubscriptionAmendment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un otrosi. Documento inmutable: se emite y ya no cambia. */
public record SubscriptionAmendmentDto(Long id, Long companyId, Long subscriptionId,
        String amendmentNumber, AmendmentType amendmentType, LocalDate effectiveDate, String reason,
        Long requestedByEmployeeId, Long requestedBySystemUserId, BigDecimal prorationAmount,
        BigDecimal monthlyDeltaAmount, Long quoteId, String clientRequestId,
        LocalDateTime createdDate) {

    public static SubscriptionAmendmentDto from(SubscriptionAmendment amendment) {
        return new SubscriptionAmendmentDto(amendment.getId(), amendment.getCompanyId(),
                amendment.getSubscriptionId(), amendment.getAmendmentNumber(),
                amendment.getAmendmentType(), amendment.getEffectiveDate(), amendment.getReason(),
                amendment.getRequestedByEmployeeId(), amendment.getRequestedBySystemUserId(),
                amendment.getProrationAmount(), amendment.getMonthlyDeltaAmount(),
                amendment.getQuoteId(), amendment.getClientRequestId(), amendment.getCreatedDate());
    }
}
