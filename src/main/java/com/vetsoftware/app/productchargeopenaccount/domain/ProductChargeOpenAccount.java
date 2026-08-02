package com.vetsoftware.app.productchargeopenaccount.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductChargeOpenAccount {
  private Long id;
  private AnimalRef animal;
  private ProductRef product;

  /** Precio unitario congelado al momento de crear el cargo (snapshot, CON IVA incluido). */
  private final BigDecimal unitPrice;

  /** Cantidad vendida; el total del cargo es unitPrice * quantity. */
  private final int quantity;

  /**
   * Impuesto congelado heredado del catálogo del producto; null si el producto no aplica impuesto.
   */
  private final TaxRef tax;

  private final boolean hasTax;
  private final BigDecimal taxPercentage;
  private final String taxName;

  /**
   * Esquema tributario congelado del catálogo: "IVA" o "INC"; null si el producto no aplica
   * impuesto.
   */
  private final String taxScheme;

  /**
   * Tratamiento tributario congelado del catálogo (GRAVADO/EXENTO/EXCLUIDO/INC). Distingue EXENTO
   * (IVA 0%) de EXCLUIDO (sin esquema) al emitir el documento del cierre. Null en cargos previos a
   * esta columna.
   */
  private final String taxTreatment;

  /**
   * Desglose tributario persistido: el precio incluye IVA → base = total / (1 + tasa), tax = total
   * - base.
   */
  private final BigDecimal baseAmount;

  private final BigDecimal taxAmount;
  private final BigDecimal totalAmount;
  private OpenAccountRef openAccount;
  private EmployeeRef createdBy;
  private final LocalDateTime createdDate;
  private boolean enabled;
  private boolean voided;
  private EmployeeRef voidedBy;
  private LocalDateTime voidedAt;
  private String voidReason;

  /**
   * Idempotency key (UUID del cliente): deduplica reintentos del mismo cargo. Nullable (legacy/sin
   * id).
   */
  private final String clientRequestId;

  public ProductChargeOpenAccount(
      Long id,
      AnimalRef animal,
      ProductRef product,
      BigDecimal unitPrice,
      int quantity,
      TaxRef tax,
      boolean hasTax,
      BigDecimal taxPercentage,
      String taxName,
      String taxScheme,
      String taxTreatment,
      BigDecimal baseAmount,
      BigDecimal taxAmount,
      BigDecimal totalAmount,
      OpenAccountRef openAccount,
      EmployeeRef createdBy,
      LocalDateTime createdDate,
      boolean enabled,
      boolean voided,
      EmployeeRef voidedBy,
      LocalDateTime voidedAt,
      String voidReason,
      String clientRequestId) {
    validate(animal, product, openAccount, unitPrice);
    if (quantity < 1) throw new IllegalArgumentException("quantity must be at least 1");
    this.id = id;
    this.animal = animal;
    this.product = product;
    this.unitPrice = unitPrice;
    this.quantity = quantity;
    this.tax = tax;
    this.hasTax = hasTax;
    this.taxPercentage = taxPercentage;
    this.taxName = taxName;
    this.taxScheme = taxScheme;
    this.taxTreatment = taxTreatment;
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
    this.clientRequestId = clientRequestId;
  }

  /** Compat (cargo sin impuesto): base = total = unitPrice, tax = 0. */
  public ProductChargeOpenAccount(
      Long id,
      AnimalRef animal,
      ProductRef product,
      BigDecimal unitPrice,
      OpenAccountRef openAccount,
      EmployeeRef createdBy,
      LocalDateTime createdDate,
      boolean enabled,
      boolean voided,
      EmployeeRef voidedBy,
      LocalDateTime voidedAt,
      String voidReason) {
    this(
        id,
        animal,
        product,
        unitPrice,
        1,
        null,
        false,
        null,
        null,
        null,
        null,
        Money.scaled(unitPrice),
        Money.zero(),
        Money.scaled(unitPrice),
        openAccount,
        createdBy,
        createdDate,
        enabled,
        voided,
        voidedBy,
        voidedAt,
        voidReason,
        null);
  }

  /** Compat (cargo activo sin anular ni impuesto). */
  public ProductChargeOpenAccount(
      Long id,
      AnimalRef animal,
      ProductRef product,
      BigDecimal unitPrice,
      OpenAccountRef openAccount,
      EmployeeRef createdBy,
      LocalDateTime createdDate,
      boolean enabled) {
    this(
        id,
        animal,
        product,
        unitPrice,
        openAccount,
        createdBy,
        createdDate,
        enabled,
        false,
        null,
        null,
        null);
  }

  public static ProductChargeOpenAccount create(
      AnimalRef animal,
      ProductRef product,
      int quantity,
      OpenAccountRef openAccount,
      EmployeeRef createdBy,
      String clientRequestId) {
    // Congela el precio de venta vigente del producto: el total de la cuenta no debe
    // cambiar si el catálogo se edita después.
    BigDecimal unitPrice =
        product == null || product.salePrice() == null ? BigDecimal.ZERO : product.salePrice();
    // El impuesto se hereda del catálogo del producto y se congela. El precio incluye IVA.
    boolean hasTax = product != null && product.hasTax() && product.tax() != null;
    TaxRef tax = hasTax ? product.tax() : null;
    BigDecimal percentage = hasTax ? tax.percentage() : null;
    String taxName = hasTax ? tax.name() : null;
    String taxScheme = hasTax ? tax.scheme() : null;
    // El tratamiento (incl. EXENTO/EXCLUIDO) se congela aunque no haya impuesto monetario, para que
    // el
    // documento del cierre pueda distinguir exento (IVA 0%) de excluido (sin esquema).
    String taxTreatment = product == null ? null : product.taxTreatment();
    BigDecimal total = Money.multiply(unitPrice, BigDecimal.valueOf(quantity));
    BigDecimal base = Money.extractBase(total, percentage);
    BigDecimal taxAmount = total.subtract(base);
    return new ProductChargeOpenAccount(
        null,
        animal,
        product,
        unitPrice,
        quantity,
        tax,
        hasTax,
        percentage,
        taxName,
        taxScheme,
        taxTreatment,
        base,
        taxAmount,
        total,
        openAccount,
        createdBy,
        LocalDateTime.now(),
        true,
        false,
        null,
        null,
        null,
        clientRequestId);
  }

  public void update(AnimalRef animal, ProductRef product, OpenAccountRef openAccount) {
    validate(animal, product, openAccount, this.unitPrice);
    this.animal = animal;
    this.product = product;
    this.openAccount = openAccount;
  }

  /**
   * Anula el cargo dejando la fila visible (no toca {@code enabled}): registra quién lo anuló,
   * cuándo y el motivo obligatorio. Un cargo ya anulado no puede volver a anularse. El total de la
   * cuenta deja de contar este cargo (lo excluye la query de suma con voided = false).
   */
  public void voidCharge(EmployeeRef voidedBy, String reason) {
    if (this.voided) throw new ProductChargeOpenAccountAlreadyVoidedException(this.id);
    if (voidedBy == null) throw new IllegalArgumentException("voidedBy is required");
    if (reason == null || reason.isBlank())
      throw new IllegalArgumentException("reason is required to void");
    this.voided = true;
    this.voidedBy = voidedBy;
    this.voidedAt = LocalDateTime.now();
    this.voidReason = reason;
  }

  private static void validate(
      AnimalRef animal, ProductRef product, OpenAccountRef openAccount, BigDecimal unitPrice) {
    if (animal == null) throw new IllegalArgumentException("animal is required");
    if (product == null) throw new IllegalArgumentException("product is required");
    if (openAccount == null) throw new IllegalArgumentException("openAccount is required");
    if (unitPrice == null) throw new IllegalArgumentException("unitPrice is required");
    if (unitPrice.compareTo(BigDecimal.ZERO) < 0)
      throw new IllegalArgumentException("unitPrice cannot be negative");
  }

  public Long getId() {
    return id;
  }

  public AnimalRef getAnimal() {
    return animal;
  }

  public ProductRef getProduct() {
    return product;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public int getQuantity() {
    return quantity;
  }

  public TaxRef getTax() {
    return tax;
  }

  public boolean isHasTax() {
    return hasTax;
  }

  public BigDecimal getTaxPercentage() {
    return taxPercentage;
  }

  public String getTaxName() {
    return taxName;
  }

  public String getTaxScheme() {
    return taxScheme;
  }

  public String getTaxTreatment() {
    return taxTreatment;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public BigDecimal getTaxAmount() {
    return taxAmount;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public OpenAccountRef getOpenAccount() {
    return openAccount;
  }

  public EmployeeRef getCreatedBy() {
    return createdBy;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isVoided() {
    return voided;
  }

  public EmployeeRef getVoidedBy() {
    return voidedBy;
  }

  public LocalDateTime getVoidedAt() {
    return voidedAt;
  }

  public String getVoidReason() {
    return voidReason;
  }

  public String getClientRequestId() {
    return clientRequestId;
  }
}
