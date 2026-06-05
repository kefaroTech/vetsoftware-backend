package com.vetsoftware.app.productchargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateProductChargeOpenAccountRequest(
        @NotNull Long animalId,
        @NotNull Long productId,
        @NotNull Long openAccountId
) {}
