package com.vetsoftware.app.spa.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateSpaRequest(
    @NotNull LocalDate date,
    @NotNull Long spaTypeId,
    @NotBlank @Size(max = 2000) String reason,
    @NotBlank @Size(max = 2000) String details,
    @NotBlank @Size(max = 2000) String observations,
    @NotNull Long animalId) {}
