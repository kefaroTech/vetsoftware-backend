package com.vetsoftware.app.cashregister.infrastructure.persistence;

import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movimiento de caja (append-only). El {@code amount} es positivo; el signo lo da el {@code type}. */
@Entity
@Table(name = "cash_movement")
public class CashMovementJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CashSessionJpaEntity session;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CashMovementType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 10)
    private CashPaymentMethod method;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 20)
    private CashReferenceType referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_by_employee_id")
    private Long createdByEmployeeId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "note", length = 255)
    private String note;

    protected CashMovementJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CashSessionJpaEntity getSession() { return session; }
    public void setSession(CashSessionJpaEntity session) { this.session = session; }
    public CashMovementType getType() { return type; }
    public void setType(CashMovementType type) { this.type = type; }
    public CashPaymentMethod getMethod() { return method; }
    public void setMethod(CashPaymentMethod method) { this.method = method; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public CashReferenceType getReferenceType() { return referenceType; }
    public void setReferenceType(CashReferenceType referenceType) { this.referenceType = referenceType; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public Long getCreatedByEmployeeId() { return createdByEmployeeId; }
    public void setCreatedByEmployeeId(Long createdByEmployeeId) { this.createdByEmployeeId = createdByEmployeeId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
