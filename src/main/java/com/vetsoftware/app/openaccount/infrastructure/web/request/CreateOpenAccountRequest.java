package com.vetsoftware.app.openaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateOpenAccountRequest(
        @NotNull Long ownerId,
        // Opcional: sede de la cuenta. Si no se envía, se usa la sede "Principal" de la empresa.
        Long branchId
) {}
