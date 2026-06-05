package com.vetsoftware.app.productchargeopenaccount.infrastructure.web.response;

import java.time.LocalDateTime;

public record ProductChargeOpenAccountResponse(
        Long id,
        AnimalSummary animal,
        ProductSummary product,
        OpenAccountSummary openAccount,
        EmployeeSummary createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {}
