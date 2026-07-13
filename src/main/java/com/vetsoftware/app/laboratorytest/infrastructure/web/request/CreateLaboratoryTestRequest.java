package com.vetsoftware.app.laboratorytest.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateLaboratoryTestRequest(
        @NotNull LocalDate date,
        @NotNull Long testTypeId,
        @NotNull @Min(1) Integer quantity,
        @Size(max = 2000) String diagnosis,
        String status,
        String prioridad,
        @NotNull Long animalId,
        Long consultationId,
        // Sede de la muestra (default = sede activa por defecto si no viene). La bandeja se scopea por ella.
        Long branchId,
        Long processedById,
        LocalDateTime processedDate
) {}
