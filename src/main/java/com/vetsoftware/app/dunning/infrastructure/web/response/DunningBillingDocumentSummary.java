package com.vetsoftware.app.dunning.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record DunningBillingDocumentSummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId, String documentNumber,
        BigDecimal balanceAmount) {
}
