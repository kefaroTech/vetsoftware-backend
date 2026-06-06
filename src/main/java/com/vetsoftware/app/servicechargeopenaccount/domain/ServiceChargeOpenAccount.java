package com.vetsoftware.app.servicechargeopenaccount.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ServiceChargeOpenAccount {
    private Long id;
    private AnimalRef animal;
    private ServiceRef service;
    /** Precio unitario congelado al momento de crear el cargo (snapshot). */
    private final BigDecimal unitPrice;
    private OpenAccountRef openAccount;
    private final EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;
    private boolean voided;
    private EmployeeRef voidedBy;
    private LocalDateTime voidedAt;
    private String voidReason;

    public ServiceChargeOpenAccount(Long id, AnimalRef animal, ServiceRef service, BigDecimal unitPrice,
                                    OpenAccountRef openAccount, EmployeeRef createdBy,
                                    LocalDateTime createdDate, boolean enabled,
                                    boolean voided, EmployeeRef voidedBy, LocalDateTime voidedAt,
                                    String voidReason) {
        validate(animal, service, openAccount, unitPrice);
        this.id = id;
        this.animal = animal;
        this.service = service;
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

    public static ServiceChargeOpenAccount create(AnimalRef animal, ServiceRef service,
                                                  OpenAccountRef openAccount, EmployeeRef createdBy) {
        // Congela el precio vigente del servicio: el total de la cuenta no debe cambiar
        // si el catálogo se edita después.
        BigDecimal unitPrice = service == null || service.price() == null
            ? BigDecimal.ZERO : service.price();
        return new ServiceChargeOpenAccount(null, animal, service, unitPrice, openAccount, createdBy,
                                            LocalDateTime.now(), true, false, null, null, null);
    }

    public void update(AnimalRef animal, ServiceRef service, OpenAccountRef openAccount) {
        validate(animal, service, openAccount, this.unitPrice);
        this.animal = animal;
        this.service = service;
        this.openAccount = openAccount;
    }

    /**
     * Anula el cargo dejando la fila visible (no toca {@code enabled}): registra quién lo anuló,
     * cuándo y el motivo obligatorio. Un cargo ya anulado no puede volver a anularse. El total de
     * la cuenta deja de contar este cargo (lo excluye la query de suma con voided = false).
     */
    public void voidCharge(EmployeeRef voidedBy, String reason) {
        if (this.voided) throw new ServiceChargeOpenAccountAlreadyVoidedException(this.id);
        if (voidedBy == null) throw new IllegalArgumentException("voidedBy is required");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason is required to void");
        this.voided = true;
        this.voidedBy = voidedBy;
        this.voidedAt = LocalDateTime.now();
        this.voidReason = reason;
    }

    private static void validate(AnimalRef animal, ServiceRef service, OpenAccountRef openAccount,
                                 BigDecimal unitPrice) {
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (service == null) throw new IllegalArgumentException("service is required");
        if (openAccount == null) throw new IllegalArgumentException("openAccount is required");
        if (unitPrice == null) throw new IllegalArgumentException("unitPrice is required");
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("unitPrice cannot be negative");
    }

    public Long getId() { return id; }
    public AnimalRef getAnimal() { return animal; }
    public ServiceRef getService() { return service; }
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
