package com.vetsoftware.app.productchargeopenaccount.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductChargeOpenAccount {
    private Long id;
    private AnimalRef animal;
    private ProductRef product;
    /** Precio unitario congelado al momento de crear el cargo (snapshot). */
    private final BigDecimal unitPrice;
    private OpenAccountRef openAccount;
    private EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;
    private boolean voided;
    private EmployeeRef voidedBy;
    private LocalDateTime voidedAt;
    private String voidReason;

    public ProductChargeOpenAccount(Long id, AnimalRef animal, ProductRef product, BigDecimal unitPrice,
                                    OpenAccountRef openAccount, EmployeeRef createdBy,
                                    LocalDateTime createdDate, boolean enabled,
                                    boolean voided, EmployeeRef voidedBy, LocalDateTime voidedAt,
                                    String voidReason) {
        validate(animal, product, openAccount, unitPrice);
        this.id = id;
        this.animal = animal;
        this.product = product;
        this.unitPrice = unitPrice;
        this.openAccount = openAccount;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.voided = voided;
        this.voidedBy = voidedBy;
        this.voidedAt = voidedAt;
        this.voidReason = voidReason;
    }

    public static ProductChargeOpenAccount create(AnimalRef animal, ProductRef product, OpenAccountRef openAccount,
                                                  EmployeeRef createdBy) {
        // Congela el precio de venta vigente del producto: el total de la cuenta no debe
        // cambiar si el catálogo se edita después.
        BigDecimal unitPrice = product == null || product.salePrice() == null
            ? BigDecimal.ZERO : product.salePrice();
        return new ProductChargeOpenAccount(null, animal, product, unitPrice, openAccount, createdBy,
            LocalDateTime.now(), true, false, null, null, null);
    }

    public void update(AnimalRef animal, ProductRef product, OpenAccountRef openAccount) {
        validate(animal, product, openAccount, this.unitPrice);
        this.animal = animal;
        this.product = product;
        this.openAccount = openAccount;
    }

    /**
     * Anula el cargo dejando la fila visible (no toca {@code enabled}): registra quién lo anuló,
     * cuándo y el motivo obligatorio. Un cargo ya anulado no puede volver a anularse. El total de
     * la cuenta deja de contar este cargo (lo excluye la query de suma con voided = false).
     */
    public void voidCharge(EmployeeRef voidedBy, String reason) {
        if (this.voided) throw new ProductChargeOpenAccountAlreadyVoidedException(this.id);
        if (voidedBy == null) throw new IllegalArgumentException("voidedBy is required");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason is required to void");
        this.voided = true;
        this.voidedBy = voidedBy;
        this.voidedAt = LocalDateTime.now();
        this.voidReason = reason;
    }

    private static void validate(AnimalRef animal, ProductRef product, OpenAccountRef openAccount,
                                 BigDecimal unitPrice) {
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (product == null) throw new IllegalArgumentException("product is required");
        if (openAccount == null) throw new IllegalArgumentException("openAccount is required");
        if (unitPrice == null) throw new IllegalArgumentException("unitPrice is required");
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("unitPrice cannot be negative");
    }

    public Long getId() { return id; }
    public AnimalRef getAnimal() { return animal; }
    public ProductRef getProduct() { return product; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public OpenAccountRef getOpenAccount() { return openAccount; }
    public EmployeeRef getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
    public boolean isVoided() { return voided; }
    public EmployeeRef getVoidedBy() { return voidedBy; }
    public LocalDateTime getVoidedAt() { return voidedAt; }
    public String getVoidReason() { return voidReason; }
}
