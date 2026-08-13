package com.vetsoftware.app.purchaseorder.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SupplierSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
