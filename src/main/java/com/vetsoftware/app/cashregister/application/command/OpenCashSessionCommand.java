package com.vetsoftware.app.cashregister.application.command;

import java.math.BigDecimal;

/** Abrir una sesión de caja en una sede y terminal administrable con la base inicial. */
public record OpenCashSessionCommand(
    Long companyId,
    Long branchId,
    Long terminalId,
    BigDecimal openingFloat,
    Long openedByEmployeeId,
    String note) {}
