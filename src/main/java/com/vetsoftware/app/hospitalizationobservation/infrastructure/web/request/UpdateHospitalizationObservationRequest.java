package com.vetsoftware.app.hospitalizationobservation.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateHospitalizationObservationRequest(
        @NotBlank @Size(max = 2000) String description) {
}
