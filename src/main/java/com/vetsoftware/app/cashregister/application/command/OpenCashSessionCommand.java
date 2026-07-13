package com.vetsoftware.app.cashregister.application.command;

import java.math.BigDecimal;

/** Abrir una sesión de caja en una sede con la base inicial. {@code terminal} nullable → "principal". */
public record OpenCashSessionCommand(Long companyId, Long branchId, String terminal, BigDecimal openingFloat,
                                     Long openedByEmployeeId, String note) {}
