package com.vetsoftware.app.company.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CompanyResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String identifier, String address,
        String contactNumber, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CitySummary city,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) MembershipSummary membership,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
