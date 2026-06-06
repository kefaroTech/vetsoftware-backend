package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceChargeOpenAccountResponse(
        Long id,
        AnimalSummary animal,
        ServiceSummary service,
        BigDecimal unitPrice,
        OpenAccountSummary openAccount,
        EmployeeSummary createdBy,
        LocalDateTime createdDate,
        boolean enabled,
        boolean voided,
        EmployeeSummary voidedBy,
        LocalDateTime voidedAt,
        String voidReason
) {}
