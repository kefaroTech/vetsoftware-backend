package com.vetsoftware.app.cashregister.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sesión de caja (aggregate root): control del dinero físico de una sede desde su apertura con base hasta el cierre
 * con conteo. Agrupa los {@link CashMovement} (append-only: ventas, abonos, ingresos/retiros/gastos manuales,
 * reversas) y, al cerrar, los {@link CashSessionCount} (esperado vs contado por método). Una sola sesión
 * {@link CashSessionStatus#OPEN} por {@code (empresa, sede, terminal)}.
 */
public class CashSession {

    /** Terminal por defecto cuando el caller no especifica una (caja única por sede). */
    public static final String DEFAULT_TERMINAL = "principal";

    private Long id;
    private final Long companyId;
    private final Long branchId;
    private final String terminal;
    private final Long openedByEmployeeId;
    private final LocalDateTime openedAt;
    private final BigDecimal openingFloat;
    private CashSessionStatus status;
    private Long closedByEmployeeId;
    private LocalDateTime closedAt;
    private String note;
    private Long version;
    private final List<CashMovement> movements;
    private final List<CashSessionCount> counts;

    public CashSession(Long id, Long companyId, Long branchId, String terminal, Long openedByEmployeeId,
                       LocalDateTime openedAt, BigDecimal openingFloat, CashSessionStatus status,
                       Long closedByEmployeeId, LocalDateTime closedAt, String note, Long version,
                       List<CashMovement> movements, List<CashSessionCount> counts) {
        if (companyId == null) throw new IllegalArgumentException("companyId is required");
        if (branchId == null) throw new IllegalArgumentException("branchId is required");
        if (openingFloat == null || openingFloat.signum() < 0)
            throw new IllegalArgumentException("openingFloat must be >= 0");
        if (status == null) throw new IllegalArgumentException("status is required");
        this.id = id;
        this.companyId = companyId;
        this.branchId = branchId;
        this.terminal = normalizeTerminal(terminal);
        this.openedByEmployeeId = openedByEmployeeId;
        this.openedAt = openedAt;
        this.openingFloat = openingFloat;
        this.status = status;
        this.closedByEmployeeId = closedByEmployeeId;
        this.closedAt = closedAt;
        this.note = note;
        this.version = version;
        this.movements = new ArrayList<>(movements == null ? List.of() : movements);
        this.counts = new ArrayList<>(counts == null ? List.of() : counts);
    }

    /** Abre una nueva sesión OPEN con la base inicial. */
    public static CashSession open(Long companyId, Long branchId, String terminal, Long openedByEmployeeId,
                                   BigDecimal openingFloat, String note) {
        return new CashSession(null, companyId, branchId, terminal, openedByEmployeeId, LocalDateTime.now(),
            openingFloat, CashSessionStatus.OPEN, null, null, note, null, new ArrayList<>(), new ArrayList<>());
    }

    private static String normalizeTerminal(String terminal) {
        return (terminal == null || terminal.isBlank()) ? DEFAULT_TERMINAL : terminal.trim();
    }

    /** Agrega un movimiento (solo con la sesión abierta). Las correcciones son movimientos nuevos, nunca updates. */
    public void addMovement(CashMovement movement) {
        if (status != CashSessionStatus.OPEN) throw new CashSessionClosedException(id);
        movements.add(movement);
    }

    /**
     * Cierra la sesión: fija estado/fecha/responsable y materializa el conteo por método (esperado del dominio vs
     * contado declarado → diferencia). Para los métodos con total esperado que no se declararon, se asume que cuadran
     * (contado = esperado, diferencia 0).
     */
    public void close(Long closedByEmployeeId, Map<CashPaymentMethod, BigDecimal> countedByMethod, String closeNote) {
        if (status != CashSessionStatus.OPEN) throw new CashSessionClosedException(id);
        Map<CashPaymentMethod, BigDecimal> expected = expectedByMethod();
        Map<CashPaymentMethod, BigDecimal> counted = countedByMethod == null ? Map.of() : countedByMethod;

        // Union de métodos: los que tienen total esperado + los declarados en el conteo (p.ej. efectivo contado
        // sin movimientos previos). Orden estable para un arqueo reproducible.
        Map<CashPaymentMethod, BigDecimal> union = new LinkedHashMap<>(expected);
        for (CashPaymentMethod m : counted.keySet()) union.putIfAbsent(m, BigDecimal.ZERO);

        counts.clear();
        for (CashPaymentMethod method : union.keySet()) {
            BigDecimal exp = expected.getOrDefault(method, BigDecimal.ZERO);
            BigDecimal cnt = counted.getOrDefault(method, exp);
            counts.add(CashSessionCount.create(method, exp, cnt));
        }
        this.status = CashSessionStatus.CLOSED;
        this.closedByEmployeeId = closedByEmployeeId;
        this.closedAt = LocalDateTime.now();
        if (closeNote != null && !closeNote.isBlank()) this.note = closeNote;
    }

    /**
     * ¿Ya existe en la sesión un movimiento para (referencia, método, tipo)? Base de la idempotencia de la
     * orquestación: no registrar dos veces la misma venta/abono ni su reversa (el índice único de la BD es la red).
     */
    public boolean hasReferencedMovement(CashReferenceType referenceType, Long referenceId, CashPaymentMethod method,
                                         CashMovementType type) {
        return movements.stream().anyMatch(m -> m.getReferenceType() == referenceType
            && java.util.Objects.equals(m.getReferenceId(), referenceId)
            && m.getMethod() == method && m.getType() == type);
    }

    /** Total esperado por método = Σ movimientos con signo, más la base inicial para el EFECTIVO. */
    public Map<CashPaymentMethod, BigDecimal> expectedByMethod() {
        Map<CashPaymentMethod, BigDecimal> totals = new LinkedHashMap<>();
        totals.put(CashPaymentMethod.CASH, openingFloat);
        for (CashMovement m : movements) {
            totals.merge(m.getMethod(), m.signedAmount(), BigDecimal::add);
        }
        return totals;
    }

    public boolean isOpen() {
        return status == CashSessionStatus.OPEN;
    }

    public Long getId() { return id; }
    public void assignId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public Long getBranchId() { return branchId; }
    public String getTerminal() { return terminal; }
    public Long getOpenedByEmployeeId() { return openedByEmployeeId; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public BigDecimal getOpeningFloat() { return openingFloat; }
    public CashSessionStatus getStatus() { return status; }
    public Long getClosedByEmployeeId() { return closedByEmployeeId; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getNote() { return note; }
    public Long getVersion() { return version; }
    public List<CashMovement> getMovements() { return List.copyOf(movements); }
    public List<CashSessionCount> getCounts() { return List.copyOf(counts); }
}
