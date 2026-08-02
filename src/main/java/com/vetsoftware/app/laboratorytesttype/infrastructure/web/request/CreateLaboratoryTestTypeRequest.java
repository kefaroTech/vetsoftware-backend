package com.vetsoftware.app.laboratorytesttype.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLaboratoryTestTypeRequest(@NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description, boolean general) {
}
