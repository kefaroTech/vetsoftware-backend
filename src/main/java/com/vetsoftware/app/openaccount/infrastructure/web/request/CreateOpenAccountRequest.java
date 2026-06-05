package com.vetsoftware.app.openaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record CreateOpenAccountRequest(
        @NotNull Long ownerId
) {}
