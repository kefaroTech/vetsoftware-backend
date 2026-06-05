package com.vetsoftware.app.openaccount.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OpenAccountResponse(
        Long id,
        OwnerSummary owner,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        CompanySummary company,
        EmployeeSummary createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {}
