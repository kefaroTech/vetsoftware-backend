package com.vetsoftware.app.openaccount.application.dto;

import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OpenAccountDto(
    Long id,
    OwnerSummaryDto owner,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    CompanySummaryDto company,
    BranchSummaryDto branch,
    OpenAccountStatus status,
    EmployeeSummaryDto createdBy,
    LocalDateTime createdDate,
    boolean enabled,
    EmployeeSummaryDto closedBy,
    LocalDateTime closedAt,
    String closeReason,
    boolean reversed,
    LocalDateTime reversedAt,
    // Versión optimista (@Version). El front la reenvía como expectedVersion en las mutaciones
    // de la cuenta para detección temprana de conflicto de concurrencia.
    Long version) {
  public static OpenAccountDto from(OpenAccount openAccount) {
    return new OpenAccountDto(
        openAccount.getId(),
        OwnerSummaryDto.from(openAccount.getOwner()),
        openAccount.getTotalAmount(),
        openAccount.getPaidAmount(),
        openAccount.getOutstandingAmount(),
        CompanySummaryDto.from(openAccount.getCompany()),
        new BranchSummaryDto(
            openAccount.getBranch().id(),
            openAccount.getBranch().name(),
            openAccount.getBranch().code()),
        openAccount.getStatus(),
        EmployeeSummaryDto.from(openAccount.getCreatedBy()),
        openAccount.getCreatedDate(),
        openAccount.isEnabled(),
        openAccount.getClosedBy() != null
            ? EmployeeSummaryDto.from(openAccount.getClosedBy())
            : null,
        openAccount.getClosedAt(),
        openAccount.getCloseReason(),
        openAccount.isReversed(),
        openAccount.getReversedAt(),
        openAccount.getVersion());
  }
}
