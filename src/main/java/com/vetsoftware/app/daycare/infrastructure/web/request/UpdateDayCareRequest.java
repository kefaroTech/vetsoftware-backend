package com.vetsoftware.app.daycare.infrastructure.web.request;

import com.vetsoftware.app.daycare.domain.DayCareType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateDayCareRequest(
        @NotNull LocalDate date,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull DayCareType type,
        @Size(max = 1000) String objects,
        @Size(max = 2000) String observations,
        @NotNull Long animalId,
        @NotNull Long companyId
) {}
