package com.vetsoftware.app.medicationschedule.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RescheduleMedicationScheduleRequest(@NotNull LocalDateTime newDateTime, String mode) {
}
