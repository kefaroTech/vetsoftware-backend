package com.vetsoftware.app.generalchargeopenaccount.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class GeneralChargeOpenAccount {
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Long id;
    private String name;
    private BigDecimal unitAmount;
    private BigDecimal quantity;
    private TaxRef tax;
    private boolean hasTax;
    /** Porcentaje de impuesto congelado al crear/actualizar el cargo; null si no aplica impuesto. */
    private BigDecimal taxPercentage;
    /** Nombre del impuesto congelado: sobrevive aunque el catálogo deshabilite el impuesto. */
    private String taxName;
    /** Esquema tributario congelado: "IVA" o "INC"; null si no aplica impuesto. */
    private String taxScheme;
    /** Desglose tributario persistido: el monto incluye IVA → base = total / (1 + tasa), iva = total - base. */
    private BigDecimal baseAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private OpenAccountRef openAccount;
    private EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;
    private boolean voided;
    private EmployeeRef voidedBy;
    private LocalDateTime voidedAt;
    private String voidReason;

    public GeneralChargeOpenAccount(Long id, String name, BigDecimal unitAmount, BigDecimal quantity,
                                    TaxRef tax, boolean hasTax, BigDecimal taxPercentage, String taxName,
                                    String taxScheme, BigDecimal baseAmount, BigDecimal taxAmount,
                                    BigDecimal totalAmount,
                                    OpenAccountRef openAccount, EmployeeRef createdBy,
                                    LocalDateTime createdDate, boolean enabled,
                                    boolean voided, EmployeeRef voidedBy, LocalDateTime voidedAt,
                                    String voidReason) {
        validate(name, unitAmount, quantity, openAccount);
        this.id = id;
        this.name = name;
        this.unitAmount = unitAmount;
        this.quantity = quantity;
        this.tax = tax;
        this.hasTax = hasTax;
        this.taxPercentage = taxPercentage;
        this.taxName = taxName;
        this.taxScheme = taxScheme;
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

    public static GeneralChargeOpenAccount create(String name, BigDecimal unitAmount, BigDecimal quantity,
                                                  TaxRef tax, OpenAccountRef openAccount,
                                                  EmployeeRef createdBy) {
        // hasTax es derivado: aplica impuesto si y solo si tiene un impuesto asignado.
        boolean hasTax = tax != null;
        BigDecimal pct = snapshotTaxPercentage(tax, hasTax);
        BigDecimal total = grossTotal(unitAmount, quantity);
        BigDecimal base = extractBase(total, pct);
        return new GeneralChargeOpenAccount(null, name, unitAmount, quantity, tax, hasTax, pct,
                snapshotTaxName(tax, hasTax), snapshotTaxScheme(tax, hasTax), base, total.subtract(base), total,
                openAccount, createdBy, LocalDateTime.now(), true,
                false, null, null, null);
    }

    /**
     * Anula el cargo dejando la fila visible (no toca {@code enabled}): registra quién lo anuló,
     * cuándo y el motivo obligatorio. Un cargo ya anulado no puede volver a anularse. El total de
     * la cuenta deja de contar este cargo (lo excluye la query de suma con voided = false).
     */
    public void voidCharge(EmployeeRef voidedBy, String reason) {
        if (this.voided) throw new GeneralChargeOpenAccountAlreadyVoidedException(this.id);
        if (voidedBy == null) throw new IllegalArgumentException("voidedBy is required");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason is required to void");
        this.voided = true;
        this.voidedBy = voidedBy;
        this.voidedAt = LocalDateTime.now();
        this.voidReason = reason;
    }

    public void update(String name, BigDecimal unitAmount, BigDecimal quantity, TaxRef tax,
                       OpenAccountRef openAccount) {
        validate(name, unitAmount, quantity, openAccount);
        // hasTax es derivado: aplica impuesto si y solo si tiene un impuesto asignado.
        boolean hasTax = tax != null;
        this.name = name;
        this.unitAmount = unitAmount;
        this.quantity = quantity;
        this.tax = tax;
        this.hasTax = hasTax;
        this.taxPercentage = snapshotTaxPercentage(tax, hasTax);
        this.taxName = snapshotTaxName(tax, hasTax);
        this.taxScheme = snapshotTaxScheme(tax, hasTax);
        this.totalAmount = grossTotal(unitAmount, quantity);
        this.baseAmount = extractBase(this.totalAmount, this.taxPercentage);
        this.taxAmount = this.totalAmount.subtract(this.baseAmount);
        this.openAccount = openAccount;
    }

    /** Congela el % de impuesto vigente: el total no debe cambiar si el catálogo de impuestos se edita. */
    private static BigDecimal snapshotTaxPercentage(TaxRef tax, boolean hasTax) {
        return hasTax && tax != null ? tax.percentage() : null;
    }

    private static String snapshotTaxName(TaxRef tax, boolean hasTax) {
        return hasTax && tax != null ? tax.name() : null;
    }

    /** Congela el esquema tributario (IVA/INC) del impuesto asignado; null si no aplica impuesto. */
    private static String snapshotTaxScheme(TaxRef tax, boolean hasTax) {
        return hasTax && tax != null ? tax.scheme() : null;
    }

    /** Total bruto (IVA incluido) = unitAmount * quantity. */
    private static BigDecimal grossTotal(BigDecimal unitAmount, BigDecimal quantity) {
        return unitAmount.multiply(quantity).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Extrae la base gravable de un total CON IVA incluido: base = total / (1 + tasa/100). */
    private static BigDecimal extractBase(BigDecimal total, BigDecimal percentage) {
        if (percentage == null || percentage.signum() == 0) return total;
        BigDecimal factor = BigDecimal.ONE.add(percentage.divide(HUNDRED, 6, RoundingMode.HALF_UP));
        return total.divide(factor, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Monto efectivo que el cargo aporta al total de la cuenta (= total_amount persistido). */
    public BigDecimal effectiveAmount() {
        return totalAmount;
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
    public BigDecimal getTaxPercentage() { return taxPercentage; }
    public String getTaxName() { return taxName; }
    public String getTaxScheme() { return taxScheme; }
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
