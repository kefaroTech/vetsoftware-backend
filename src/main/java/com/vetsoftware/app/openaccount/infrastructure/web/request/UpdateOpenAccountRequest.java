package com.vetsoftware.app.openaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

public record UpdateOpenAccountRequest(
        @NotNull Long ownerId
) {}
