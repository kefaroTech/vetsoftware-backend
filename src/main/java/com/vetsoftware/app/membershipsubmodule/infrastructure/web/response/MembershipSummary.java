package com.vetsoftware.app.membershipsubmodule.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record MembershipSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
