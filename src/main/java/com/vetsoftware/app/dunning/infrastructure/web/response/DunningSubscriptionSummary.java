package com.vetsoftware.app.dunning.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Companion local: este slice no expone la Response de {@code subscription}.
 */
public record DunningSubscriptionSummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        String subscriptionNumber, String status) {
}
