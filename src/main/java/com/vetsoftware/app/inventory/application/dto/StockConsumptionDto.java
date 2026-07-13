package com.vetsoftware.app.inventory.application.dto;

import java.math.BigDecimal;

/** Detalle de una salida por lote: cuánto se descontó de qué lote y a qué costo (para COGS/margen). */
public record StockConsumptionDto(Long lotId, int quantity, BigDecimal unitCost) {}
