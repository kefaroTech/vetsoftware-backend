package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateHospitalizationProgressNoteRequest(
        @NotBlank @Size(max = 2000) String description) {
}
