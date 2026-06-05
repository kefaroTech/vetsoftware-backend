package com.vetsoftware.app.debtopenaccount.application.command;

import java.math.BigDecimal;

public record UpdateDebtOpenAccountCommand(
        Long id,
        BigDecimal amount,
        String paymentMethod,
        Long openAccountId,
        Long companyId
) {}
