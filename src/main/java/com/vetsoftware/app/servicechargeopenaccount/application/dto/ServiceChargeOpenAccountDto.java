package com.vetsoftware.app.servicechargeopenaccount.application.dto;

import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceChargeOpenAccountDto(
    Long id,
    AnimalSummaryDto animal,
    ServiceSummaryDto service,
    BigDecimal unitPrice,
    boolean hasTax,
    BigDecimal taxPercentage,
    String taxName,
    BigDecimal baseAmount,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    OpenAccountSummaryDto openAccount,
    EmployeeSummaryDto createdBy,
    LocalDateTime createdDate,
    boolean enabled,
    boolean voided,
    EmployeeSummaryDto voidedBy,
    LocalDateTime voidedAt,
    String voidReason) {
  public static ServiceChargeOpenAccountDto from(ServiceChargeOpenAccount charge) {
    return new ServiceChargeOpenAccountDto(
        charge.getId(),
        AnimalSummaryDto.from(charge.getAnimal()),
        ServiceSummaryDto.from(charge.getService()),
        charge.getUnitPrice(),
        charge.isHasTax(),
        charge.getTaxPercentage(),
        charge.getTaxName(),
        charge.getBaseAmount(),
        charge.getTaxAmount(),
        charge.getTotalAmount(),
        OpenAccountSummaryDto.from(charge.getOpenAccount()),
        charge.getCreatedBy() == null ? null : EmployeeSummaryDto.from(charge.getCreatedBy()),
        charge.getCreatedDate(),
        charge.isEnabled(),
        charge.isVoided(),
        charge.getVoidedBy() == null ? null : EmployeeSummaryDto.from(charge.getVoidedBy()),
        charge.getVoidedAt(),
        charge.getVoidReason());
  }
}
