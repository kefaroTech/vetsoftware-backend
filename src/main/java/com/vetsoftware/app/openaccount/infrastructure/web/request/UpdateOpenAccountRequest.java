package com.vetsoftware.app.openaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record UpdateOpenAccountRequest(
        @NotNull Long ownerId,
        // Versión optimista de la cuenta que vio el front (opt-in) para detección temprana de conflicto.
        Long expectedVersion
) {}
