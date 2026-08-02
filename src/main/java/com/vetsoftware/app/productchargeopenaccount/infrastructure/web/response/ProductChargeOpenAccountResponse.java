package com.vetsoftware.app.productchargeopenaccount.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductChargeOpenAccountResponse(Long id, AnimalSummary animal,
        ProductSummary product, BigDecimal unitPrice, int quantity, boolean hasTax,
        BigDecimal taxPercentage, String taxName, BigDecimal baseAmount, BigDecimal taxAmount,
        BigDecimal totalAmount, OpenAccountSummary openAccount, EmployeeSummary createdBy,
        LocalDateTime createdDate, boolean enabled, boolean voided, EmployeeSummary voidedBy,
        LocalDateTime voidedAt, String voidReason) {
}
