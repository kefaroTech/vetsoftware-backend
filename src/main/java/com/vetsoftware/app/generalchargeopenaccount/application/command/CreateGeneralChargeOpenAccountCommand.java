package com.vetsoftware.app.generalchargeopenaccount.application.command;

import java.math.BigDecimal;

public record CreateGeneralChargeOpenAccountCommand(String name, BigDecimal unitAmount,
        BigDecimal quantity, Long taxId, Long openAccountId, Long companyId, Long createdById,
        String clientRequestId,
        // Versión esperada de la cuenta (opt-in) para detección temprana de conflicto.
        // null = sin
        // chequeo.
        Long expectedVersion) {
}
