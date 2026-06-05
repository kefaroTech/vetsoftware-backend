package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record UpdateServiceChargeOpenAccountRequest(
        @NotNull Long animalId,
        @NotNull Long serviceId,
        @NotNull Long openAccountId
) {}
