package com.vetsoftware.app.openaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeOpenAccountStatusRequest(
        @NotBlank String status,
        // Opcional a nivel HTTP: la obligatoriedad del motivo en CANCEL la enforza el dominio.
        String reason
) {}
