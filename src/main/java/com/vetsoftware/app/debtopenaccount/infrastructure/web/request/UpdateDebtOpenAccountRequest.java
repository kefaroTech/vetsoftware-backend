package com.vetsoftware.app.debtopenaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateDebtOpenAccountRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String paymentMethod,
        @NotNull Long openAccountId
) {}
