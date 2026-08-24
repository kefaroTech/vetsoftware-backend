package com.vetsoftware.app.quote.application.dto;

import com.vetsoftware.app.quote.domain.QuoteLine;
import java.math.BigDecimal;

/**
 * El renglon tal como se proyecta hacia fuera: SIEMPRE desde las copias
 * congeladas de la linea, nunca resolviendo nada contra el catalogo.
 *
 * <p>
 * Salen las TRES cantidades -{@code contractedQuantity} lo que pidio el
 * cliente, {@code includedQuantity} lo que la tarifa traia incluido y
 * {@code quantity} lo que se cobra-. Ensenar solo el resultado obliga a hacer
 * arqueologia el dia que el cliente pregunte por que le cobran uno y no tres,
 * que es exactamente lo que este modelo existe para evitar.
 */
public record QuoteLineDto(Long id, int lineNumber, Long catalogItemId, String itemCode,
        String itemName, String itemType, int contractedQuantity, int includedQuantity,
        int quantity, BigDecimal unitAmount, BigDecimal grossAmount, BigDecimal discountPercent,
        BigDecimal discountAmount, BigDecimal taxRate, String taxTreatment, BigDecimal taxAmount,
        BigDecimal lineTotal, boolean enabled) {

    public static QuoteLineDto from(QuoteLine line) {
        return new QuoteLineDto(line.getId(), line.getLineNumber(), line.getCatalogItemId(),
                line.getItemCode(), line.getItemName(), line.getItemType().name(),
                line.getContractedQuantity(), line.getIncludedQuantity(), line.getQuantity(),
                line.getUnitAmount(), line.grossAmount(), line.getDiscountPercent(),
                line.getDiscountAmount(), line.getTaxRate(), line.getTaxTreatment().name(),
                line.getTaxAmount(), line.getLineTotal(), line.isEnabled());
    }
}
