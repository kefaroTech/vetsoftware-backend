package com.vetsoftware.app.generalchargeopenaccount.application.command;

import java.math.BigDecimal;

public record CreateGeneralChargeOpenAccountCommand(
        String name,
        BigDecimal unitAmount,
        BigDecimal quantity,
        Long taxId,
        boolean hasTax,
        Long openAccountId,
        Long companyId,
        Long createdById
) {}
