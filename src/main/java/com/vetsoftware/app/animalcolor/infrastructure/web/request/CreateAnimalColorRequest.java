package com.vetsoftware.app.animalcolor.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAnimalColorRequest(@NotBlank @Size(max = 100) String name,
        @NotNull Long specieId) {
}
