package com.vetsoftware.app.animalcolor.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAnimalColorRequest(@NotBlank @Size(max = 100) String name) {}
