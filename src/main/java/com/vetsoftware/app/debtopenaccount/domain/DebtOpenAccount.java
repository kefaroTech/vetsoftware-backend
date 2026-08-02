package com.vetsoftware.app.debtopenaccount.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DebtOpenAccount {
  private Long id;
  private BigDecimal amount;
  private PaymentMethod paymentMethod;
  private OpenAccountRef openAccount;
  private final EmployeeRef createdBy;
  private final LocalDateTime createdDate;
  private boolean enabled;
  private boolean voided;
  private EmployeeRef voidedBy;
  private LocalDateTime voidedAt;
  private String voidReason;

  /**
   * Idempotency key (UUID del cliente): deduplica reintentos del mismo cobro. Nullable (legacy/sin
   * id).
   */
  private final String clientRequestId;

  public DebtOpenAccount(
      Long id,
      BigDecimal amount,
      PaymentMethod paymentMethod,
      OpenAccountRef openAccount,
      EmployeeRef createdBy,
      LocalDateTime createdDate,
      boolean enabled,
      boolean voided,
      EmployeeRef voidedBy,
      LocalDateTime voidedAt,
      String voidReason,
      String clientRequestId) {
    validate(amount, paymentMethod, openAccount);
    this.id = id;
    this.amount = amount;
    this.paymentMethod = paymentMethod;
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

  public static DebtOpenAccount create(
      BigDecimal amount,
      PaymentMethod paymentMethod,
      OpenAccountRef openAccount,
      EmployeeRef createdBy,
      String clientRequestId) {
    return new DebtOpenAccount(
        null,
        amount,
        paymentMethod,
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

  public void update(BigDecimal amount, PaymentMethod paymentMethod, OpenAccountRef openAccount) {
    validate(amount, paymentMethod, openAccount);
    this.amount = amount;
    this.paymentMethod = paymentMethod;
    this.openAccount = openAccount;
  }

  /**
   * Anula el abono dejando la fila visible (no toca {@code enabled}): registra quién lo anuló,
   * cuándo y el motivo obligatorio. Un abono ya anulado no puede volver a anularse.
   */
  public void voidPayment(EmployeeRef voidedBy, String reason) {
    if (this.voided) throw new DebtOpenAccountAlreadyVoidedException(this.id);
    if (voidedBy == null) throw new IllegalArgumentException("voidedBy is required");
    if (reason == null || reason.isBlank())
      throw new IllegalArgumentException("reason is required to void");
    this.voided = true;
    this.voidedBy = voidedBy;
    this.voidedAt = LocalDateTime.now();
    this.voidReason = reason;
  }

  private static void validate(
      BigDecimal amount, PaymentMethod paymentMethod, OpenAccountRef openAccount) {
    if (amount == null) throw new IllegalArgumentException("amount is required");
    if (amount.compareTo(BigDecimal.ZERO) <= 0)
      throw new IllegalArgumentException("amount must be positive");
    if (paymentMethod == null) throw new IllegalArgumentException("paymentMethod is required");
    if (openAccount == null) throw new IllegalArgumentException("openAccount is required");
  }

  public Long getId() {
    return id;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
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
