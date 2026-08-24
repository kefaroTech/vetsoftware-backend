package com.vetsoftware.app.subscriptionpayment.infrastructure.web.response;

import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillingDocumentApplicationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BillingDocumentSummary targetDocument,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ApplicationSourceKind sourceKind,
        Long paymentId, BillingDocumentSummary sourceDocument,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal appliedAmount,
        Long reversalOfId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime appliedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {
}
