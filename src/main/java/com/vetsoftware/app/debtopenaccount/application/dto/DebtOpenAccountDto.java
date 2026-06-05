package com.vetsoftware.app.debtopenaccount.application.dto;

import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DebtOpenAccountDto(
        Long id,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        OpenAccountSummaryDto openAccount,
        EmployeeSummaryDto createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static DebtOpenAccountDto from(DebtOpenAccount debtOpenAccount) {
        return new DebtOpenAccountDto(
                debtOpenAccount.getId(),
                debtOpenAccount.getAmount(),
                debtOpenAccount.getPaymentMethod(),
                OpenAccountSummaryDto.from(debtOpenAccount.getOpenAccount()),
                debtOpenAccount.getCreatedBy() == null ? null
                    : EmployeeSummaryDto.from(debtOpenAccount.getCreatedBy()),
                debtOpenAccount.getCreatedDate(),
                debtOpenAccount.isEnabled());
    }
}
