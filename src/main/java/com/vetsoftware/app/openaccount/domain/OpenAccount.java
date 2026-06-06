package com.vetsoftware.app.openaccount.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OpenAccount {
    private Long id;
    private OwnerRef owner;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private CompanyRef company;
    private OpenAccountStatus status;
    private EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;
    private EmployeeRef closedBy;
    private LocalDateTime closedAt;
    private String closeReason;
    private Long version;

    public OpenAccount(Long id, OwnerRef owner, BigDecimal totalAmount, BigDecimal paidAmount,
                       BigDecimal outstandingAmount, CompanyRef company, OpenAccountStatus status,
                       EmployeeRef createdBy, LocalDateTime createdDate, boolean enabled,
                       EmployeeRef closedBy, LocalDateTime closedAt, String closeReason, Long version) {
        validate(owner, totalAmount, paidAmount, outstandingAmount, company, status, createdBy);
        this.id = id;
        this.owner = owner;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.outstandingAmount = outstandingAmount;
        this.company = company;
        this.status = status;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
        this.closeReason = closeReason;
        this.version = version;
    }

    public static OpenAccount create(OwnerRef owner, CompanyRef company, EmployeeRef createdBy) {
        return new OpenAccount(null, owner, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                               company, OpenAccountStatus.OPEN, createdBy, LocalDateTime.now(), true,
                               null, null, null, null);
    }

    public void update(OwnerRef owner) {
        if (owner == null) throw new IllegalArgumentException("owner is required");
        this.owner = owner;
    }

    /**
     * Cambia el estado de la cuenta. Reglas de negocio:
     * - Solo desde OPEN (CLOSE y CANCEL son terminales).
     * - Solo hacia CLOSE o CANCEL.
     * - CANCEL exige motivo (anulación/incobrable = pérdida, debe justificarse). CLOSE no.
     * Registra quién, cuándo y por qué (trazabilidad contable).
     */
    public void changeStatus(OpenAccountStatus newStatus, EmployeeRef closedBy, String reason) {
        if (newStatus == null) throw new IllegalArgumentException("status is required");
        if (this.status != OpenAccountStatus.OPEN
                || (newStatus != OpenAccountStatus.CLOSE && newStatus != OpenAccountStatus.CANCEL)) {
            throw new InvalidOpenAccountStatusTransitionException(this.status, newStatus);
        }
        if (newStatus == OpenAccountStatus.CANCEL && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("reason is required to cancel");
        }
        this.status = newStatus;
        this.closedBy = closedBy;
        this.closedAt = LocalDateTime.now();
        this.closeReason = reason;
    }

    public void recalculate(BigDecimal total, BigDecimal paid) {
        if (total == null) throw new IllegalArgumentException("totalAmount is required");
        if (total.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("totalAmount cannot be negative");
        if (paid == null) throw new IllegalArgumentException("paidAmount is required");
        if (paid.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("paidAmount cannot be negative");
        this.totalAmount = total;
        this.paidAmount = paid;
        this.outstandingAmount = total.subtract(paid);
    }

    private static void validate(OwnerRef owner, BigDecimal totalAmount, BigDecimal paidAmount,
                                 BigDecimal outstandingAmount, CompanyRef company,
                                 OpenAccountStatus status, EmployeeRef createdBy) {
        if (owner == null) throw new IllegalArgumentException("owner is required");
        if (totalAmount == null) throw new IllegalArgumentException("totalAmount is required");
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("totalAmount cannot be negative");
        if (paidAmount == null) throw new IllegalArgumentException("paidAmount is required");
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("paidAmount cannot be negative");
        if (outstandingAmount == null) throw new IllegalArgumentException("outstandingAmount is required");
        if (company == null) throw new IllegalArgumentException("company is required");
        if (status == null) throw new IllegalArgumentException("status is required");
        if (createdBy == null) throw new IllegalArgumentException("createdBy is required");
    }

    public Long getId() { return id; }
    public OwnerRef getOwner() { return owner; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public CompanyRef getCompany() { return company; }
    public OpenAccountStatus getStatus() { return status; }
    public EmployeeRef getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public EmployeeRef getClosedBy() { return closedBy; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getCloseReason() { return closeReason; }
    public Long getVersion() { return version; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
