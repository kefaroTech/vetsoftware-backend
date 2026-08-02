package com.vetsoftware.app.cashregister.infrastructure.web.request;

import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

/** Cierre de caja con el conteo declarado por método. */
public record CloseCashSessionRequest(@Valid List<CountLine> counts, String note) {

    /**
     * Conteo de un método: cuánto se contó físicamente (efectivo) o concilió
     * (no-efectivo).
     */
    public record CountLine(@NotNull CashPaymentMethod method,
            @NotNull @PositiveOrZero BigDecimal countedAmount) {
    }
}
