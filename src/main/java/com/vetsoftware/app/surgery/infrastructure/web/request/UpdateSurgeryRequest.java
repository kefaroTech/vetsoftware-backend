package com.vetsoftware.app.surgery.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateSurgeryRequest(
    @NotNull LocalDate date,
    @NotNull Long surgeryTypeId,
    @NotBlank @Size(max = 2000) String description,
    @Size(max = 200) String medicament,
    @Size(max = 2000) String observations,
    @Size(max = 2000) String complications,
    @NotNull Long animalId,
    Long consultationId) {}
