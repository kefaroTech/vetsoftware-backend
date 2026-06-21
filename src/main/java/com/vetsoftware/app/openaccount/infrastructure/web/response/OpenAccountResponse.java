package com.vetsoftware.app.openaccount.infrastructure.web.response;

import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OpenAccountResponse(
        Long id,
        OwnerSummary owner,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        CompanySummary company,
        OpenAccountStatus status,
        EmployeeSummary createdBy,
        LocalDateTime createdDate,
        boolean enabled,
        EmployeeSummary closedBy,
        LocalDateTime closedAt,
        String closeReason,
        boolean reversed,
        LocalDateTime reversedAt,
        Long version
) {}
