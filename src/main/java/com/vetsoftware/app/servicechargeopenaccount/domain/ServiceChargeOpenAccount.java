package com.vetsoftware.app.servicechargeopenaccount.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class ServiceChargeOpenAccount {
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Long id;
    private AnimalRef animal;
    private ServiceRef service;
    /** Precio unitario congelado al momento de crear el cargo (snapshot, CON IVA incluido). */
    private final BigDecimal unitPrice;
    /** Impuesto congelado heredado del catálogo del servicio; null si el servicio no aplica impuesto. */
    private final TaxRef tax;
    private final boolean hasTax;
    private final BigDecimal taxPercentage;
    private final String taxName;
    /** Desglose tributario persistido: el precio incluye IVA → base = total / (1 + tasa), tax = total - base. */
    private final BigDecimal baseAmount;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;
    private OpenAccountRef openAccount;
    private final EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;
    private boolean voided;
    private EmployeeRef voidedBy;
    private LocalDateTime voidedAt;
    private String voidReason;

    public ServiceChargeOpenAccount(Long id, AnimalRef animal, ServiceRef service, BigDecimal unitPrice,
                                    TaxRef tax, boolean hasTax, BigDecimal taxPercentage, String taxName,
                                    BigDecimal baseAmount, BigDecimal taxAmount, BigDecimal totalAmount,
                                    OpenAccountRef openAccount, EmployeeRef createdBy,
                                    LocalDateTime createdDate, boolean enabled,
                                    boolean voided, EmployeeRef voidedBy, LocalDateTime voidedAt,
                                    String voidReason) {
        validate(animal, service, openAccount, unitPrice);
        this.id = id;
        this.animal = animal;
        this.service = service;
        this.unitPrice = unitPrice;
        this.tax = tax;
        this.hasTax = hasTax;
        this.taxPercentage = taxPercentage;
        this.taxName = taxName;
        this.baseAmount = baseAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.openAccount = openAccount;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.voided = voided;
        this.voidedBy = voidedBy;
        this.voidedAt = voidedAt;
        this.voidReason = voidReason;
    }

    /** Compat (cargo sin impuesto): base = total = unitPrice, tax = 0. */
    public ServiceChargeOpenAccount(Long id, AnimalRef animal, ServiceRef service, BigDecimal unitPrice,
                                    OpenAccountRef openAccount, EmployeeRef createdBy,
                                    LocalDateTime createdDate, boolean enabled,
                                    boolean voided, EmployeeRef voidedBy, LocalDateTime voidedAt,
                                    String voidReason) {
        this(id, animal, service, unitPrice, null, false, null, null,
            scaled(unitPrice), zero(), scaled(unitPrice),
            openAccount, createdBy, createdDate, enabled, voided, voidedBy, voidedAt, voidReason);
    }

    /** Compat (cargo activo sin anular ni impuesto). */
    public ServiceChargeOpenAccount(Long id, AnimalRef animal, ServiceRef service, BigDecimal unitPrice,
                                    OpenAccountRef openAccount, EmployeeRef createdBy,
                                    LocalDateTime createdDate, boolean enabled) {
        this(id, animal, service, unitPrice, openAccount, createdBy, createdDate, enabled,
            false, null, null, null);
    }

    public static ServiceChargeOpenAccount create(AnimalRef animal, ServiceRef service,
                                                  OpenAccountRef openAccount, EmployeeRef createdBy) {
        // Congela el precio vigente del servicio: el total de la cuenta no debe cambiar
        // si el catálogo se edita después.
        BigDecimal unitPrice = service == null || service.price() == null
            ? BigDecimal.ZERO : service.price();
        // El impuesto se hereda del catálogo del servicio y se congela. El precio incluye IVA.
        boolean hasTax = service != null && service.hasTax() && service.tax() != null;
        TaxRef tax = hasTax ? service.tax() : null;
        BigDecimal percentage = hasTax ? tax.percentage() : null;
        String taxName = hasTax ? tax.name() : null;
        BigDecimal total = unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal base = extractBase(total, percentage);
        BigDecimal taxAmount = total.subtract(base);
        return new ServiceChargeOpenAccount(null, animal, service, unitPrice, tax, hasTax, percentage, taxName,
            base, taxAmount, total, openAccount, createdBy, LocalDateTime.now(), true, false, null, null, null);
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

    /** Extrae la base gravable de un precio CON IVA incluido: base = total / (1 + tasa/100). */
    private static BigDecimal extractBase(BigDecimal total, BigDecimal percentage) {
        if (percentage == null || percentage.signum() == 0) return total;
        BigDecimal factor = BigDecimal.ONE.add(percentage.divide(HUNDRED, 6, RoundingMode.HALF_UP));
        return total.divide(factor, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal scaled(BigDecimal value) {
        return value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE);
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
    public TaxRef getTax() { return tax; }
    public boolean isHasTax() { return hasTax; }
    public BigDecimal getTaxPercentage() { return taxPercentage; }
    public String getTaxName() { return taxName; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
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
