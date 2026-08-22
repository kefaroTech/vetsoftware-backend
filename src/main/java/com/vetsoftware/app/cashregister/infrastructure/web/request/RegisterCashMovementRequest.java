package com.vetsoftware.app.cashregister.infrastructure.web.request;

import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Movimiento manual de caja: solo MANUAL_IN/WITHDRAWAL/EXPENSE (el service
 * rechaza los demás).
 */
public record RegisterCashMovementRequest(
        @NotNull(message = "Debes seleccionar el tipo de movimiento.") CashMovementType type,
        @NotNull(message = "Debes seleccionar el método de pago.") CashPaymentMethod method,
        @NotNull(message = "El monto es obligatorio.") @Positive(message = "El monto debe ser mayor que cero.") BigDecimal amount,
        String note) {
}
