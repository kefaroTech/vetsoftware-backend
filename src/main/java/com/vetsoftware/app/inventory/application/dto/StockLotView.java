package com.vetsoftware.app.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lote disponible de un producto en una sede (para trazabilidad/FEFO/UI). */
public record StockLotView(
    Long lotId,
    String lotNumber,
    LocalDate expireDate,
    int quantityAvailable,
    BigDecimal unitCost) {}
