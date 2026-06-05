package com.vetsoftware.app.productchargeopenaccount.domain;

import java.time.LocalDateTime;

public class ProductChargeOpenAccount {
    private Long id;
    private AnimalRef animal;
    private ProductRef product;
    private OpenAccountRef openAccount;
    private EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public ProductChargeOpenAccount(Long id, AnimalRef animal, ProductRef product, OpenAccountRef openAccount,
                                    EmployeeRef createdBy, LocalDateTime createdDate, boolean enabled) {
        validate(animal, product, openAccount);
        this.id = id;
        this.animal = animal;
        this.product = product;
        this.openAccount = openAccount;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static ProductChargeOpenAccount create(AnimalRef animal, ProductRef product, OpenAccountRef openAccount,
                                                  EmployeeRef createdBy) {
        return new ProductChargeOpenAccount(null, animal, product, openAccount, createdBy,
            LocalDateTime.now(), true);
    }

    public void update(AnimalRef animal, ProductRef product, OpenAccountRef openAccount) {
        validate(animal, product, openAccount);
        this.animal = animal;
        this.product = product;
        this.openAccount = openAccount;
    }

    private static void validate(AnimalRef animal, ProductRef product, OpenAccountRef openAccount) {
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (product == null) throw new IllegalArgumentException("product is required");
        if (openAccount == null) throw new IllegalArgumentException("openAccount is required");
    }

    public Long getId() { return id; }
    public AnimalRef getAnimal() { return animal; }
    public ProductRef getProduct() { return product; }
    public OpenAccountRef getOpenAccount() { return openAccount; }
    public EmployeeRef getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
