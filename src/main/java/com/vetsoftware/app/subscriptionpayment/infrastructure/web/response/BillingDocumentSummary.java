package com.vetsoftware.app.subscriptionpayment.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Companion local del documento de cobro. No importa la respuesta de
 * {@code subscriptionbilling}: el vertical slicing lo prohibe, y ademas asi el
 * JSON de este endpoint no cambia de forma cada vez que aquel slice anade un
 * campo.
 */
public record BillingDocumentSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId, String documentNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String documentKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal totalAmount,
        BigDecimal balanceAmount) {
}
