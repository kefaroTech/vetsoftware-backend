package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record HospitalizationSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date) {
}
