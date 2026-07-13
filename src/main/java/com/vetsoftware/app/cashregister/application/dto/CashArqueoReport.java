package com.vetsoftware.app.cashregister.application.dto;

import com.vetsoftware.app.cashregister.domain.CashMovement;
import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionCount;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reporte de arqueo de una sesión de caja: cabecera + desglose por método (base, ventas, abonos, ingresos, retiros,
 * gastos, reversas, esperado vs contado y diferencia) + listado de movimientos. Insumo del export CSV/PDF y del BI.
 */
public record CashArqueoReport(
        Long sessionId, Long branchId, String terminal, CashSessionStatus status,
        Long openedByEmployeeId, LocalDateTime openedAt, Long closedByEmployeeId, LocalDateTime closedAt,
        String note, BigDecimal openingFloat, LocalDateTime generatedAt,
        List<MethodRow> methods, List<CashMovementView> movements,
        BigDecimal totalExpected, BigDecimal totalCounted, BigDecimal totalDifference) {

    /** Desglose de un método de pago: categorías de movimiento + conciliación (esperado/contado/diferencia). */
    public record MethodRow(CashPaymentMethod method, BigDecimal opening, BigDecimal salesIn, BigDecimal accountIn,
                            BigDecimal manualIn, BigDecimal withdrawals, BigDecimal expenses, BigDecimal voidOut,
                            BigDecimal expected, BigDecimal counted, BigDecimal difference) {}

    public static CashArqueoReport from(CashSession s) {
        Map<CashPaymentMethod, BigDecimal> expectedByMethod = s.expectedByMethod();
        boolean closed = s.getStatus() == CashSessionStatus.CLOSED;
        List<MethodRow> rows = new ArrayList<>();
        BigDecimal totalExpected = BigDecimal.ZERO;
        BigDecimal totalCounted = BigDecimal.ZERO;
        BigDecimal totalDifference = BigDecimal.ZERO;

        for (CashPaymentMethod m : CashPaymentMethod.values()) {
            BigDecimal opening = m == CashPaymentMethod.CASH ? s.getOpeningFloat() : BigDecimal.ZERO;
            BigDecimal salesIn = sum(s, m, CashMovementType.SALE_IN);
            BigDecimal accountIn = sum(s, m, CashMovementType.OPEN_ACCOUNT_IN);
            BigDecimal manualIn = sum(s, m, CashMovementType.MANUAL_IN);
            BigDecimal withdrawals = sum(s, m, CashMovementType.WITHDRAWAL);
            BigDecimal expenses = sum(s, m, CashMovementType.EXPENSE);
            BigDecimal voidOut = sum(s, m, CashMovementType.VOID_OUT);
            BigDecimal expected = expectedByMethod.getOrDefault(m, BigDecimal.ZERO);
            CashSessionCount count = s.getCounts().stream().filter(c -> c.getMethod() == m).findFirst().orElse(null);
            BigDecimal counted = count != null ? count.getCountedAmount() : null;
            BigDecimal difference = count != null ? count.difference() : null;

            boolean hasActivity = opening.signum() != 0 || salesIn.signum() != 0 || accountIn.signum() != 0
                || manualIn.signum() != 0 || withdrawals.signum() != 0 || expenses.signum() != 0
                || voidOut.signum() != 0 || expected.signum() != 0 || count != null;
            if (m != CashPaymentMethod.CASH && !hasActivity) continue;

            rows.add(new MethodRow(m, opening, salesIn, accountIn, manualIn, withdrawals, expenses, voidOut,
                expected, counted, difference));
            totalExpected = totalExpected.add(expected);
            if (counted != null) totalCounted = totalCounted.add(counted);
            if (difference != null) totalDifference = totalDifference.add(difference);
        }

        return new CashArqueoReport(s.getId(), s.getBranchId(), s.getTerminal(), s.getStatus(),
            s.getOpenedByEmployeeId(), s.getOpenedAt(), s.getClosedByEmployeeId(), s.getClosedAt(), s.getNote(),
            s.getOpeningFloat(), LocalDateTime.now(), rows,
            s.getMovements().stream().map(CashMovementView::from).toList(),
            totalExpected, closed ? totalCounted : null, closed ? totalDifference : null);
    }

    private static BigDecimal sum(CashSession s, CashPaymentMethod method, CashMovementType type) {
        return s.getMovements().stream()
            .filter(mv -> mv.getMethod() == method && mv.getType() == type)
            .map(CashMovement::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
