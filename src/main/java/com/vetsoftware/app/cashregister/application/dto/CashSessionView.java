package com.vetsoftware.app.cashregister.application.dto;

import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Salida de una sesión de caja. En el detalle / current va completa (totales por método + movimientos + counts); en
 * el listado va como {@link #summary} (sin movimientos ni counts, solo la cabecera).
 */
public record CashSessionView(Long id, Long branchId, String terminal, CashSessionStatus status,
                              Long openedByEmployeeId, LocalDateTime openedAt, BigDecimal openingFloat,
                              Long closedByEmployeeId, LocalDateTime closedAt, String note, Long version,
                              List<MethodTotalView> totals, List<CashMovementView> movements,
                              List<CashSessionCountView> counts) {

    /** Vista completa (detalle / current / respuesta a open/close/movimiento) con totales, movimientos y counts. */
    public static CashSessionView from(CashSession s) {
        List<MethodTotalView> totals = s.expectedByMethod().entrySet().stream()
            .map(e -> new MethodTotalView(e.getKey(), e.getValue()))
            .toList();
        return new CashSessionView(s.getId(), s.getBranchId(), s.getTerminal(), s.getStatus(),
            s.getOpenedByEmployeeId(), s.getOpenedAt(), s.getOpeningFloat(), s.getClosedByEmployeeId(),
            s.getClosedAt(), s.getNote(), s.getVersion(), totals,
            s.getMovements().stream().map(CashMovementView::from).toList(),
            s.getCounts().stream().map(CashSessionCountView::from).toList());
    }

    /** Vista de resumen (listado / historial): solo la cabecera, sin movimientos ni counts. */
    public static CashSessionView summary(Long id, Long branchId, String terminal, CashSessionStatus status,
                                          Long openedByEmployeeId, LocalDateTime openedAt, BigDecimal openingFloat,
                                          Long closedByEmployeeId, LocalDateTime closedAt, String note, Long version) {
        return new CashSessionView(id, branchId, terminal, status, openedByEmployeeId, openedAt, openingFloat,
            closedByEmployeeId, closedAt, note, version, List.of(), List.of(), List.of());
    }
}
