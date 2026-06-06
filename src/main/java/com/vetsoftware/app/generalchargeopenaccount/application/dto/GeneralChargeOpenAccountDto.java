package com.vetsoftware.app.generalchargeopenaccount.application.dto;

import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GeneralChargeOpenAccountDto(
        Long id,
        String name,
        BigDecimal unitAmount,
        BigDecimal quantity,
        TaxSummaryDto tax,
        boolean hasTax,
        BigDecimal taxPercentage,
        OpenAccountSummaryDto openAccount,
        EmployeeSummaryDto createdBy,
        LocalDateTime createdDate,
        boolean enabled,
        boolean voided,
        EmployeeSummaryDto voidedBy,
        LocalDateTime voidedAt,
        String voidReason
) {
    public static GeneralChargeOpenAccountDto from(GeneralChargeOpenAccount charge) {
        return new GeneralChargeOpenAccountDto(
                charge.getId(),
                charge.getName(),
                charge.getUnitAmount(),
                charge.getQuantity(),
                charge.getTax() == null ? null : TaxSummaryDto.from(charge.getTax()),
                charge.isHasTax(),
                charge.getTaxPercentage(),
                OpenAccountSummaryDto.from(charge.getOpenAccount()),
                EmployeeSummaryDto.from(charge.getCreatedBy()),
                charge.getCreatedDate(),
                charge.isEnabled(),
                charge.isVoided(),
                charge.getVoidedBy() == null ? null : EmployeeSummaryDto.from(charge.getVoidedBy()),
                charge.getVoidedAt(),
                charge.getVoidReason());
    }
}
