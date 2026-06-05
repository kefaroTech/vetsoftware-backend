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
    private EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public OpenAccount(Long id, OwnerRef owner, BigDecimal totalAmount, BigDecimal paidAmount,
                       BigDecimal outstandingAmount, CompanyRef company, EmployeeRef createdBy,
                       LocalDateTime createdDate, boolean enabled) {
        validate(owner, totalAmount, paidAmount, outstandingAmount, company, createdBy);
        this.id = id;
        this.owner = owner;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.outstandingAmount = outstandingAmount;
        this.company = company;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static OpenAccount create(OwnerRef owner, CompanyRef company, EmployeeRef createdBy) {
        return new OpenAccount(null, owner, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                               company, createdBy, LocalDateTime.now(), true);
    }

    public void update(OwnerRef owner) {
        if (owner == null) throw new IllegalArgumentException("owner is required");
        this.owner = owner;
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
                                 BigDecimal outstandingAmount, CompanyRef company, EmployeeRef createdBy) {
        if (owner == null) throw new IllegalArgumentException("owner is required");
        if (totalAmount == null) throw new IllegalArgumentException("totalAmount is required");
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("totalAmount cannot be negative");
        if (paidAmount == null) throw new IllegalArgumentException("paidAmount is required");
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("paidAmount cannot be negative");
        if (outstandingAmount == null) throw new IllegalArgumentException("outstandingAmount is required");
        if (company == null) throw new IllegalArgumentException("company is required");
        if (createdBy == null) throw new IllegalArgumentException("createdBy is required");
    }

    public Long getId() { return id; }
    public OwnerRef getOwner() { return owner; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public CompanyRef getCompany() { return company; }
    public EmployeeRef getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
