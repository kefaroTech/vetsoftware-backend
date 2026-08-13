package com.vetsoftware.app.debtopenaccount.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record OpenAccountSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        Long companyId) {
}
