package com.vetsoftware.app.hospitalization.infrastructure.web.response;

import java.time.LocalDate;

public record ConsultationSummary(Long id, LocalDate date) {
}
