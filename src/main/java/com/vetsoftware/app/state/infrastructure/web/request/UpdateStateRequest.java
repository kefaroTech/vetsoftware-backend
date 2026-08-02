package com.vetsoftware.app.state.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStateRequest(@NotBlank @Size(max = 100) String name, @NotNull Long countryId,
        @Size(max = 2) String daneCode) {
}
