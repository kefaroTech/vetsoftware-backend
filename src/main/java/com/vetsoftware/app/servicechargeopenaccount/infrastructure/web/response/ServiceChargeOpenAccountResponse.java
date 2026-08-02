package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceChargeOpenAccountResponse(Long id, AnimalSummary animal,
        ServiceSummary service, BigDecimal unitPrice, boolean hasTax, BigDecimal taxPercentage,
        String taxName, BigDecimal baseAmount, BigDecimal taxAmount, BigDecimal totalAmount,
        OpenAccountSummary openAccount, EmployeeSummary createdBy, LocalDateTime createdDate,
        boolean enabled, boolean voided, EmployeeSummary voidedBy, LocalDateTime voidedAt,
        String voidReason) {
}
