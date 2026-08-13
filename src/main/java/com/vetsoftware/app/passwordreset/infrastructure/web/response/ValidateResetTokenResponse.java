package com.vetsoftware.app.passwordreset.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ValidateResetTokenResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean valid) {
}
