package com.vetsoftware.app.cashregister.application.command;

import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import java.math.BigDecimal;
import java.util.List;

/**
 * Cerrar una sesión de caja con el conteo declarado por método. El servicio
 * calcula esperado (dominio) vs contado (declarado) → diferencia, y persiste
 * los {@code CashSessionCount}.
 */
public record CloseCashSessionCommand(Long companyId, Long sessionId, Long closedByEmployeeId,
        String note, List<Count> counts) {

    /**
     * Conteo declarado de un método al cerrar: cuánto se contó físicamente
     * (efectivo) o concilió (no-efectivo).
     */
    public record Count(CashPaymentMethod method, BigDecimal countedAmount) {
    }
}
