package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateServiceChargeOpenAccountRequest(
        @NotNull Long animalId,
        @NotNull Long serviceId,
        @NotNull Long openAccountId
) {}
