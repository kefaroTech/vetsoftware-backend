package com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GeneralChargeOpenAccountResponse(Long id, String name, BigDecimal unitAmount,
        BigDecimal quantity, TaxSummary tax, boolean hasTax, BigDecimal taxPercentage,
        String taxName, BigDecimal baseAmount, BigDecimal taxAmount, BigDecimal totalAmount,
        OpenAccountSummary openAccount, EmployeeSummary createdBy, LocalDateTime createdDate,
        boolean enabled, boolean voided, EmployeeSummary voidedBy, LocalDateTime voidedAt,
        String voidReason) {
}
