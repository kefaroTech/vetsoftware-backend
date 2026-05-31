package com.vetsoftware.app.hospitalizationobservation.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateHospitalizationObservationRequest(
        @NotBlank @Size(max = 2000) String description,
        @NotNull Long hospitalizationId
) {}
