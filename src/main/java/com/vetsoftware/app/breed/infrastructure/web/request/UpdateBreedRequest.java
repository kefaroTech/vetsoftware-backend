package com.vetsoftware.app.breed.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBreedRequest(@NotBlank @Size(max = 100) String name, @NotNull Long specieId) {}
