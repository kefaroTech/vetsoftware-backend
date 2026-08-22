package com.vetsoftware.app.cashregister.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Apertura de caja. {@code terminal} opcional (default "principal");
 * {@code branchId} lo acota el alcance del empleado.
 */
public record OpenCashSessionRequest(@NotNull(message = "Debes seleccionar la sede.") Long branchId,
        @NotNull(message = "Debes seleccionar la terminal.") Long terminalId,
        @NotNull(message = "La base inicial es obligatoria.") @PositiveOrZero(message = "La base inicial no puede ser negativa.") BigDecimal openingFloat,
        String note) {
}
