package com.vetsoftware.app.debtopenaccount.application.command;

import java.math.BigDecimal;

public record CreateDebtOpenAccountCommand(
        BigDecimal amount,
        String paymentMethod,
        Long openAccountId,
        Long companyId,
        Long createdById
) {}
