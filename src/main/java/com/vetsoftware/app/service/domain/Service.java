package com.vetsoftware.app.service.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Service {
    private Long id;
    private String name;
    private BigDecimal price;
    private TaxTreatment taxTreatment;
    private String notes;
    private ServiceCategoryRef serviceCategory;
    private TaxRef tax;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Long updatedBy;
    private Long version;
    private boolean enabled;

    public Service(Long id, String name, BigDecimal price, TaxTreatment taxTreatment, String notes,
            ServiceCategoryRef serviceCategory, TaxRef tax, CompanyRef company,
            LocalDateTime createdDate, LocalDateTime updatedDate, Long updatedBy, Long version,
            boolean enabled) {
        validate(name, price, notes, serviceCategory, company);
        validateTaxTreatment(taxTreatment, tax);
        this.id = id;
        this.name = name;
        this.price = price;
        this.taxTreatment = taxTreatment;
        this.notes = notes;
        this.serviceCategory = serviceCategory;
        this.tax = tax;
        this.company = company;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.updatedBy = updatedBy;
        this.version = version;
        this.enabled = enabled;
    }

    public static Service create(String name, BigDecimal price, TaxTreatment taxTreatment,
            String notes, ServiceCategoryRef serviceCategory, TaxRef tax, CompanyRef company) {
        return new Service(null, name, price, taxTreatment, notes, serviceCategory, tax, company,
                LocalDateTime.now(), null, null, null, true);
    }

    public void update(String name, BigDecimal price, TaxTreatment taxTreatment, String notes,
            ServiceCategoryRef serviceCategory, TaxRef tax, CompanyRef company, Long updatedBy,
            Long expectedVersion) {
        validate(name, price, notes, serviceCategory, company);
        validateTaxTreatment(taxTreatment, tax);
        this.name = name;
        this.price = price;
        this.taxTreatment = taxTreatment;
        this.notes = notes;
        this.serviceCategory = serviceCategory;
        this.tax = tax;
        this.company = company;
        this.updatedDate = LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.version = expectedVersion;
    }

    private static void validate(String name, BigDecimal price, String notes,
            ServiceCategoryRef serviceCategory, CompanyRef company) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 100)
            throw new IllegalArgumentException("name must be 100 chars or less");
        if (price == null)
            throw new IllegalArgumentException("price is required");
        if (price.signum() < 0)
            throw new IllegalArgumentException("price cannot be negative");
        if (notes != null && notes.length() > 500)
            throw new IllegalArgumentException("notes must be 500 chars or less");
        if (serviceCategory == null)
            throw new IllegalArgumentException("serviceCategory is required");
        if (company == null)
            throw new IllegalArgumentException("company is required");
    }

    private static void validateTaxTreatment(TaxTreatment taxTreatment, TaxRef tax) {
        if (taxTreatment == null)
            throw new IllegalArgumentException("taxTreatment is required");
        switch (taxTreatment) {
            case GRAVADO, INC -> {
                if (tax == null)
                    throw new IllegalArgumentException(
                            "taxTreatment " + taxTreatment + " requires a tax");
                if (tax.percentage().signum() <= 0)
                    throw new IllegalArgumentException("taxTreatment " + taxTreatment
                            + " requires a tax percentage greater than 0");
            }
            case EXENTO, EXCLUIDO -> {
                if (tax != null)
                    throw new IllegalArgumentException(
                            "taxTreatment " + taxTreatment + " must not have a tax");
            }
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public TaxTreatment getTaxTreatment() {
        return taxTreatment;
    }

    public String getNotes() {
        return notes;
    }

    public ServiceCategoryRef getServiceCategory() {
        return serviceCategory;
    }

    public TaxRef getTax() {
        return tax;
    }

    public CompanyRef getCompany() {
        return company;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
