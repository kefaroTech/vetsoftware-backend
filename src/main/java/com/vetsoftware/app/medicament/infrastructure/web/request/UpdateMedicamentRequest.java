package com.vetsoftware.app.medicament.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMedicamentRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String description
) {}
