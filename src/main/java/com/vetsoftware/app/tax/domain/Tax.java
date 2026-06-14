package com.vetsoftware.app.tax.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Tax {
    private Long id;
    private String name;
    private BigDecimal percentage;
    private TaxScheme taxScheme;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public Tax(Long id, String name, BigDecimal percentage, TaxScheme taxScheme,
               CompanyRef company, LocalDateTime createdDate, boolean enabled) {
        validate(name, percentage, taxScheme, company);
        this.id = id;
        this.name = name;
        this.percentage = percentage;
        this.taxScheme = taxScheme;
        this.company = company;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static Tax create(String name, BigDecimal percentage, TaxScheme taxScheme, CompanyRef company) {
        return new Tax(null, name, percentage, taxScheme, company, LocalDateTime.now(), true);
    }

    public void update(String name, BigDecimal percentage, TaxScheme taxScheme, CompanyRef company) {
        validate(name, percentage, taxScheme, company);
        this.name = name;
        this.percentage = percentage;
        this.taxScheme = taxScheme;
        this.company = company;
    }

    private static void validate(String name, BigDecimal percentage, TaxScheme taxScheme, CompanyRef company) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (percentage == null) throw new IllegalArgumentException("percentage is required");
        if (percentage.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("percentage cannot be negative");
        if (percentage.compareTo(BigDecimal.valueOf(100)) > 0) throw new IllegalArgumentException("percentage cannot exceed 100");
        if (taxScheme == null) throw new IllegalArgumentException("taxScheme is required");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPercentage() { return percentage; }
    public TaxScheme getTaxScheme() { return taxScheme; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
