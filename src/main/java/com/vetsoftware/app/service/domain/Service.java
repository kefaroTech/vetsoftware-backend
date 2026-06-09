package com.vetsoftware.app.service.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Service {
    private Long id;
    private String name;
    private BigDecimal price;
    private boolean hasTax;
    private String notes;
    private ServiceCategoryRef serviceCategory;
    private TaxRef tax;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public Service(Long id, String name, BigDecimal price, boolean hasTax, String notes,
                   ServiceCategoryRef serviceCategory, TaxRef tax, CompanyRef company,
                   LocalDateTime createdDate, boolean enabled) {
        validate(name, price, notes, serviceCategory, company);
        this.id = id;
        this.name = name;
        this.price = price;
        this.hasTax = hasTax;
        this.notes = notes;
        this.serviceCategory = serviceCategory;
        this.tax = tax;
        this.company = company;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static Service create(String name, BigDecimal price, String notes,
                                 ServiceCategoryRef serviceCategory, TaxRef tax, CompanyRef company) {
        // hasTax es derivado: aplica impuesto si y solo si tiene un impuesto asignado.
        return new Service(null, name, price, tax != null, notes, serviceCategory, tax, company,
                LocalDateTime.now(), true);
    }

    public void update(String name, BigDecimal price, String notes,
                       ServiceCategoryRef serviceCategory, TaxRef tax, CompanyRef company) {
        validate(name, price, notes, serviceCategory, company);
        this.name = name;
        this.price = price;
        this.hasTax = tax != null;
        this.notes = notes;
        this.serviceCategory = serviceCategory;
        this.tax = tax;
        this.company = company;
    }

    private static void validate(String name, BigDecimal price, String notes,
                                 ServiceCategoryRef serviceCategory, CompanyRef company) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (price == null) throw new IllegalArgumentException("price is required");
        if (price.signum() < 0) throw new IllegalArgumentException("price cannot be negative");
        if (notes != null && notes.length() > 500) throw new IllegalArgumentException("notes must be 500 chars or less");
        if (serviceCategory == null) throw new IllegalArgumentException("serviceCategory is required");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public boolean isHasTax() { return hasTax; }
    public String getNotes() { return notes; }
    public ServiceCategoryRef getServiceCategory() { return serviceCategory; }
    public TaxRef getTax() { return tax; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
