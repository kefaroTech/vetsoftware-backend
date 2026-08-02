package com.vetsoftware.app.inventory.application.command;

import com.vetsoftware.app.inventory.domain.StockReferenceType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Entrada de inventario (compra/recepción): crea/acumula un lote en la sede con su costo real. */
public record RecordPurchaseCommand(
    Long companyId,
    Long branchId,
    Long productId,
    String lotNumber,
    LocalDate expireDate,
    int quantity,
    BigDecimal unitCost,
    StockReferenceType referenceType,
    Long referenceId,
    Long createdBy) {}
