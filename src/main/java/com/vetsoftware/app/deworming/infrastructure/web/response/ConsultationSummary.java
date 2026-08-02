package com.vetsoftware.app.deworming.infrastructure.web.response;

import java.time.LocalDate;

public record ConsultationSummary(Long id, LocalDate date) {
}
