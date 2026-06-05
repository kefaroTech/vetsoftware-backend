package com.vetsoftware.app.debtopenaccount.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DebtOpenAccount {
    private Long id;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private OpenAccountRef openAccount;
    private final EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public DebtOpenAccount(Long id, BigDecimal amount, PaymentMethod paymentMethod,
                           OpenAccountRef openAccount, EmployeeRef createdBy,
                           LocalDateTime createdDate, boolean enabled) {
        validate(amount, paymentMethod, openAccount);
        this.id = id;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.openAccount = openAccount;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static DebtOpenAccount create(BigDecimal amount, PaymentMethod paymentMethod,
                                         OpenAccountRef openAccount, EmployeeRef createdBy) {
        return new DebtOpenAccount(null, amount, paymentMethod, openAccount, createdBy,
            LocalDateTime.now(), true);
    }

    public void update(BigDecimal amount, PaymentMethod paymentMethod, OpenAccountRef openAccount) {
        validate(amount, paymentMethod, openAccount);
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.openAccount = openAccount;
    }

    private static void validate(BigDecimal amount, PaymentMethod paymentMethod, OpenAccountRef openAccount) {
        if (amount == null) throw new IllegalArgumentException("amount is required");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must be positive");
        if (paymentMethod == null) throw new IllegalArgumentException("paymentMethod is required");
        if (openAccount == null) throw new IllegalArgumentException("openAccount is required");
    }

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public OpenAccountRef getOpenAccount() { return openAccount; }
    public EmployeeRef getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
