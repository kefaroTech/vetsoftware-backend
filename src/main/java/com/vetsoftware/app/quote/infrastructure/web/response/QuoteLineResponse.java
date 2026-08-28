package com.vetsoftware.app.quote.infrastructure.web.response;

import java.math.BigDecimal;

/**
 * El renglon congelado tal como sale por HTTP.
 *
 * <p>
 * Van las TRES cantidades a proposito: {@code contractedQuantity} lo que pidio
 * el cliente, {@code includedQuantity} lo que la tarifa traia incluido y
 * {@code quantity} lo que se cobra. Con solo el resultado, explicarle a quien
 * reclama por que le cobran uno y no tres obliga a hacer arqueologia sobre la
 * tarifa de hoy para justificar un documento de hace un ano.
 *
 * <p>
 * {@code tierMin}/{@code tierMax} explican por que hay dos renglones del mismo
 * articulo a precios distintos: los tramos son acumulativos (D-66). Y
 * {@code taxableBase} junto con {@code discountIsConditional} explican por que
 * el impuesto no sale del importe rebajado cuando hay permanencia (D-86).
 */
public record QuoteLineResponse(Long id, int lineNumber, Long catalogItemId, String itemCode,
        String itemName, String itemType, int tierMin, Integer tierMax, int contractedQuantity,
        int includedQuantity, int quantity, BigDecimal unitAmount, BigDecimal grossAmount,
        BigDecimal discountPercent, BigDecimal discountAmount, boolean discountIsConditional,
        BigDecimal taxRate, String taxTreatment, BigDecimal taxableBase, BigDecimal taxAmount,
        BigDecimal lineTotal, boolean enabled) {
}
