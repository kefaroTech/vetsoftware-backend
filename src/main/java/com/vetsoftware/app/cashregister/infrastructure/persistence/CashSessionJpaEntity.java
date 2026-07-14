package com.vetsoftware.app.cashregister.infrastructure.persistence;

import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sesión de caja. La columna generada {@code open_marker} (= 1 solo cuando status=OPEN, NULL en otro caso) vive en la
 * BD para los índices únicos condicionales por terminal y por empleado; no se mapea aquí (ddl-auto: validate ignora
 * columnas no mapeadas). Movimientos y counts son append-only (cascade ALL, sin orphanRemoval).
 */
@Entity
@Table(name = "cash_session")
public class CashSessionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "terminal", nullable = false, length = 60)
    private String terminal;

    @Column(name = "opened_by_employee_id")
    private Long openedByEmployeeId;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "opening_float", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingFloat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private CashSessionStatus status;

    @Column(name = "closed_by_employee_id")
    private Long closedByEmployeeId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "note", length = 255)
    private String note;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<CashMovementJpaEntity> movements = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<CashSessionCountJpaEntity> counts = new ArrayList<>();

    protected CashSessionJpaEntity() {}

    public void addMovement(CashMovementJpaEntity movement) {
        movement.setSession(this);
        movements.add(movement);
    }

    public void addCount(CashSessionCountJpaEntity count) {
        count.setSession(this);
        counts.add(count);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }
    public Long getOpenedByEmployeeId() { return openedByEmployeeId; }
    public void setOpenedByEmployeeId(Long openedByEmployeeId) { this.openedByEmployeeId = openedByEmployeeId; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
    public BigDecimal getOpeningFloat() { return openingFloat; }
    public void setOpeningFloat(BigDecimal openingFloat) { this.openingFloat = openingFloat; }
    public CashSessionStatus getStatus() { return status; }
    public void setStatus(CashSessionStatus status) { this.status = status; }
    public Long getClosedByEmployeeId() { return closedByEmployeeId; }
    public void setClosedByEmployeeId(Long closedByEmployeeId) { this.closedByEmployeeId = closedByEmployeeId; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<CashMovementJpaEntity> getMovements() { return movements; }
    public List<CashSessionCountJpaEntity> getCounts() { return counts; }
}
