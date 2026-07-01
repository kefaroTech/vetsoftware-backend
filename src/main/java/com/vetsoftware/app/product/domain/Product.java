package com.vetsoftware.app.product.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
    private Long id;
    private String name;
    private String code;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private Integer currentStock;
    private Integer minStock;
    private String provider;
    private TaxTreatment taxTreatment;
    private boolean expireDate;
    private String notes;
    private ProductCategoryRef productCategory;
    private TaxRef tax;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Long updatedBy;
    private Long version;
    private boolean enabled;

    public Product(Long id, String name, String code, BigDecimal purchasePrice, BigDecimal salePrice,
                   Integer currentStock, Integer minStock, String provider, TaxTreatment taxTreatment,
                   boolean expireDate, String notes, ProductCategoryRef productCategory, TaxRef tax,
                   CompanyRef company, LocalDateTime createdDate, LocalDateTime updatedDate, Long updatedBy,
                   Long version, boolean enabled) {
        validate(name, code, purchasePrice, salePrice, currentStock, minStock, provider, notes,
                 productCategory, company);
        validateTaxTreatment(taxTreatment, tax);
        this.id = id;
        this.name = name;
        this.code = code;
        this.purchasePrice = purchasePrice;
        this.salePrice = salePrice;
        this.currentStock = currentStock;
        this.minStock = minStock;
        this.provider = provider;
        this.taxTreatment = taxTreatment;
        this.expireDate = expireDate;
        this.notes = notes;
        this.productCategory = productCategory;
        this.tax = tax;
        this.company = company;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.updatedBy = updatedBy;
        this.version = version;
        this.enabled = enabled;
    }

    public static Product create(String name, String code, BigDecimal purchasePrice, BigDecimal salePrice,
                                 Integer currentStock, Integer minStock, String provider,
                                 TaxTreatment taxTreatment, boolean expireDate, String notes,
                                 ProductCategoryRef productCategory, TaxRef tax, CompanyRef company) {
        return new Product(null, name, code, purchasePrice, salePrice, currentStock, minStock, provider,
                           taxTreatment, expireDate, notes, productCategory, tax, company,
                           LocalDateTime.now(), null, null, null, true);
    }

    public void update(String name, String code, BigDecimal purchasePrice, BigDecimal salePrice,
                       Integer currentStock, Integer minStock, String provider,
                       TaxTreatment taxTreatment, boolean expireDate, String notes,
                       ProductCategoryRef productCategory, TaxRef tax, CompanyRef company, Long updatedBy,
                       Long expectedVersion) {
        validate(name, code, purchasePrice, salePrice, currentStock, minStock, provider, notes,
                 productCategory, company);
        validateTaxTreatment(taxTreatment, tax);
        this.name = name;
        this.code = code;
        this.purchasePrice = purchasePrice;
        this.salePrice = salePrice;
        this.currentStock = currentStock;
        this.minStock = minStock;
        this.provider = provider;
        this.taxTreatment = taxTreatment;
        this.expireDate = expireDate;
        this.notes = notes;
        this.productCategory = productCategory;
        this.tax = tax;
        this.company = company;
        this.updatedDate = LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.version = expectedVersion;
    }

    private static void validate(String name, String code, BigDecimal purchasePrice, BigDecimal salePrice,
                                 Integer currentStock, Integer minStock, String provider, String notes,
                                 ProductCategoryRef productCategory, CompanyRef company) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is required");
        if (code.length() > 50) throw new IllegalArgumentException("code must be 50 chars or less");
        if (purchasePrice == null) throw new IllegalArgumentException("purchasePrice is required");
        if (purchasePrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("purchasePrice cannot be negative");
        if (salePrice == null) throw new IllegalArgumentException("salePrice is required");
        if (salePrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("salePrice cannot be negative");
        if (currentStock == null) throw new IllegalArgumentException("currentStock is required");
        if (currentStock < 0) throw new IllegalArgumentException("currentStock cannot be negative");
        if (minStock == null) throw new IllegalArgumentException("minStock is required");
        if (minStock < 0) throw new IllegalArgumentException("minStock cannot be negative");
        if (provider != null && provider.length() > 150) throw new IllegalArgumentException("provider must be 150 chars or less");
        if (notes != null && notes.length() > 500) throw new IllegalArgumentException("notes must be 500 chars or less");
        if (productCategory == null) throw new IllegalArgumentException("productCategory is required");
        if (company == null) throw new IllegalArgumentException("company is required");
    }

    private static void validateTaxTreatment(TaxTreatment taxTreatment, TaxRef tax) {
        if (taxTreatment == null) throw new IllegalArgumentException("taxTreatment is required");
        switch (taxTreatment) {
            case GRAVADO, INC -> {
                if (tax == null)
                    throw new IllegalArgumentException("taxTreatment " + taxTreatment + " requires a tax");
                if (tax.percentage().signum() <= 0)
                    throw new IllegalArgumentException("taxTreatment " + taxTreatment + " requires a tax percentage greater than 0");
            }
            case EXENTO, EXCLUIDO -> {
                if (tax != null)
                    throw new IllegalArgumentException("taxTreatment " + taxTreatment + " must not have a tax");
            }
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public Integer getCurrentStock() { return currentStock; }
    public Integer getMinStock() { return minStock; }
    public String getProvider() { return provider; }
    public TaxTreatment getTaxTreatment() { return taxTreatment; }
    public boolean isExpireDate() { return expireDate; }
    public String getNotes() { return notes; }
    public ProductCategoryRef getProductCategory() { return productCategory; }
    public TaxRef getTax() { return tax; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public Long getUpdatedBy() { return updatedBy; }
    public Long getVersion() { return version; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
