package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.response;

import java.time.LocalDateTime;

public record ServiceChargeOpenAccountResponse(
        Long id,
        AnimalSummary animal,
        ServiceSummary service,
        OpenAccountSummary openAccount,
        EmployeeSummary createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {}
