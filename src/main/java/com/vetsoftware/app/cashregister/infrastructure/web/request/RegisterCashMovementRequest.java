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
public record RegisterCashMovementRequest(@NotNull CashMovementType type,
        @NotNull CashPaymentMethod method, @NotNull @Positive BigDecimal amount, String note) {
}
