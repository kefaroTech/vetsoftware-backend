package com.vetsoftware.app.generalchargeopenaccount.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GeneralChargeOpenAccount {
    private Long id;
    private String name;
    private BigDecimal unitAmount;
    private BigDecimal quantity;
    private TaxRef tax;
    private boolean hasTax;
    private OpenAccountRef openAccount;
    private EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public GeneralChargeOpenAccount(Long id, String name, BigDecimal unitAmount, BigDecimal quantity,
                                    TaxRef tax, boolean hasTax, OpenAccountRef openAccount,
                                    EmployeeRef createdBy, LocalDateTime createdDate, boolean enabled) {
        validate(name, unitAmount, quantity, openAccount);
        this.id = id;
        this.name = name;
        this.unitAmount = unitAmount;
        this.quantity = quantity;
        this.tax = tax;
        this.hasTax = hasTax;
        this.openAccount = openAccount;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static GeneralChargeOpenAccount create(String name, BigDecimal unitAmount, BigDecimal quantity,
                                                  TaxRef tax, boolean hasTax, OpenAccountRef openAccount,
                                                  EmployeeRef createdBy) {
        return new GeneralChargeOpenAccount(null, name, unitAmount, quantity, tax, hasTax, openAccount,
                createdBy, LocalDateTime.now(), true);
    }

    public void update(String name, BigDecimal unitAmount, BigDecimal quantity, TaxRef tax,
                       boolean hasTax, OpenAccountRef openAccount) {
        validate(name, unitAmount, quantity, openAccount);
        this.name = name;
        this.unitAmount = unitAmount;
        this.quantity = quantity;
        this.tax = tax;
        this.hasTax = hasTax;
        this.openAccount = openAccount;
    }

    private static void validate(String name, BigDecimal unitAmount, BigDecimal quantity,
                                 OpenAccountRef openAccount) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 150) throw new IllegalArgumentException("name must be 150 chars or less");
        if (unitAmount == null) throw new IllegalArgumentException("unitAmount is required");
        if (unitAmount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("unitAmount cannot be negative");
        if (quantity == null) throw new IllegalArgumentException("quantity is required");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("quantity must be greater than zero");
        if (openAccount == null) throw new IllegalArgumentException("openAccount is required");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getUnitAmount() { return unitAmount; }
    public BigDecimal getQuantity() { return quantity; }
    public TaxRef getTax() { return tax; }
    public boolean isHasTax() { return hasTax; }
    public OpenAccountRef getOpenAccount() { return openAccount; }
    public EmployeeRef getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
