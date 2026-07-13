package com.vetsoftware.app.cashregister.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Apertura de caja. {@code terminal} opcional (default "principal"); {@code branchId} lo acota el alcance del empleado. */
public record OpenCashSessionRequest(
        @NotNull Long branchId,
        @NotNull @PositiveOrZero BigDecimal openingFloat,
        String terminal,
        String note) {}
