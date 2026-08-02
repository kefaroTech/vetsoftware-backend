package com.vetsoftware.app.debtopenaccount.application.command;

import java.math.BigDecimal;

public record CreateDebtOpenAccountCommand(
    BigDecimal amount,
    String paymentMethod,
    Long openAccountId,
    Long companyId,
    Long createdById,
    String clientRequestId,
    // Versión esperada de la cuenta (opt-in) para detección temprana de conflicto. null = sin
    // chequeo.
    Long expectedVersion) {}
