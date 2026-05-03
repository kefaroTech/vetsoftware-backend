package com.vetsoftware.app.specie.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSpecieRequest(
        @NotBlank @Size(max = 100) String name
) {}
