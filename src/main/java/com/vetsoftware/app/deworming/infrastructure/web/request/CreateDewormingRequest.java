package com.vetsoftware.app.deworming.infrastructure.web.request;

import com.vetsoftware.app.deworming.domain.DewormingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateDewormingRequest(@NotNull LocalDate date, LocalDate lastDeworming,
        @NotNull DewormingType type, @NotBlank @Size(max = 200) String product,
        @NotBlank @Size(max = 200) String dosage, LocalDate nextControl,
        @Size(max = 2000) String observations, @NotNull Long animalId, Long consultationId) {
}
