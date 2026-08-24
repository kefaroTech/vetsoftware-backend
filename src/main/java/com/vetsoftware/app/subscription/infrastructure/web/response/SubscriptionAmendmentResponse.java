package com.vetsoftware.app.subscription.infrastructure.web.response;

import com.vetsoftware.app.subscription.domain.AmendmentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un otrosi tal como sale por HTTP. */
public record SubscriptionAmendmentResponse(Long id, Long companyId, Long subscriptionId,
        String amendmentNumber, AmendmentType amendmentType, LocalDate effectiveDate, String reason,
        Long requestedByEmployeeId, Long requestedBySystemUserId, BigDecimal prorationAmount,
        BigDecimal monthlyDeltaAmount, Long quoteId, String clientRequestId,
        LocalDateTime createdDate) {
}
