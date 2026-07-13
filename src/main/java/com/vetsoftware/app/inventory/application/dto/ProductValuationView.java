package com.vetsoftware.app.inventory.application.dto;

import java.math.BigDecimal;

/** Valuación de un producto: unidades disponibles y su valor (Σ lote.qty × lote.costo). */
public record ProductValuationView(
        Long productId,
        String productName,
        String productCode,
        int quantity,
        BigDecimal value
) {}
