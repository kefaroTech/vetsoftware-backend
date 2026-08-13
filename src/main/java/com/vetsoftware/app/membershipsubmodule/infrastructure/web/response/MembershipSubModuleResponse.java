package com.vetsoftware.app.membershipsubmodule.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record MembershipSubModuleResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) MembershipSummary membership,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SubModuleSummary subModule,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        boolean enabled) {
}
