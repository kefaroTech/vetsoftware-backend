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
        OpenAccountStatus status,
        EmployeeSummaryDto createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static OpenAccountDto from(OpenAccount openAccount) {
        return new OpenAccountDto(
                openAccount.getId(),
                OwnerSummaryDto.from(openAccount.getOwner()),
                openAccount.getTotalAmount(),
                openAccount.getPaidAmount(),
                openAccount.getOutstandingAmount(),
                CompanySummaryDto.from(openAccount.getCompany()),
                openAccount.getStatus(),
                EmployeeSummaryDto.from(openAccount.getCreatedBy()),
                openAccount.getCreatedDate(),
                openAccount.isEnabled());
    }
}
