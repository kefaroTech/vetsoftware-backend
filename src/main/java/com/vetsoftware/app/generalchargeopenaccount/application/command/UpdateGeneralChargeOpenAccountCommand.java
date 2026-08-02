package com.vetsoftware.app.generalchargeopenaccount.application.command;

import java.math.BigDecimal;

public record UpdateGeneralChargeOpenAccountCommand(
    Long id,
    String name,
    BigDecimal unitAmount,
    BigDecimal quantity,
    Long taxId,
    Long openAccountId,
    Long companyId,
    // Versión esperada de la cuenta (opt-in) para detección temprana de conflicto. null = sin
    // chequeo.
    Long expectedVersion) {}
