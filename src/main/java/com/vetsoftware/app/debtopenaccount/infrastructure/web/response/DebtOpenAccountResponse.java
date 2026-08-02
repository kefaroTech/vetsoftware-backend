package com.vetsoftware.app.debtopenaccount.infrastructure.web.response;

import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DebtOpenAccountResponse(Long id, BigDecimal amount, PaymentMethod paymentMethod,
        OpenAccountSummary openAccount, EmployeeSummary createdBy, LocalDateTime createdDate,
        boolean enabled, boolean voided, EmployeeSummary voidedBy, LocalDateTime voidedAt,
        String voidReason) {
}
