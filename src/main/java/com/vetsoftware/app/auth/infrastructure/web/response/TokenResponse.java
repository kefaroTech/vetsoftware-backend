package com.vetsoftware.app.auth.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String token,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String type, String refreshToken) {
}
