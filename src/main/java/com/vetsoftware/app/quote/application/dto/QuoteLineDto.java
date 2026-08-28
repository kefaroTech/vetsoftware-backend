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
 *
 * <p>
 * Salen ademas el TRAMO -{@code tierMin}/{@code tierMax}, para que dos
 * renglones del mismo articulo a precios distintos se expliquen solos (D-66)- y
 * la BASE IMPONIBLE junto con la marca de descuento condicionado: con
 * permanencia, el impuesto se liquida sobre el precio de lista y no sobre el
 * rebajado (D-86), y sin ensenar la base ese numero parece un error de calculo.
 */
public record QuoteLineDto(Long id, int lineNumber, Long catalogItemId, String itemCode,
        String itemName, String itemType, int tierMin, Integer tierMax, int contractedQuantity,
        int includedQuantity, int quantity, BigDecimal unitAmount, BigDecimal grossAmount,
        BigDecimal discountPercent, BigDecimal discountAmount, boolean discountIsConditional,
        BigDecimal taxRate, String taxTreatment, BigDecimal taxableBase, BigDecimal taxAmount,
        BigDecimal lineTotal, boolean enabled) {

    public static QuoteLineDto from(QuoteLine line) {
        return new QuoteLineDto(line.getId(), line.getLineNumber(), line.getCatalogItemId(),
                line.getItemCode(), line.getItemName(), line.getItemType().name(),
                line.getTierMin(), line.getTierMax(), line.getContractedQuantity(),
                line.getIncludedQuantity(), line.getQuantity(), line.getUnitAmount(),
                line.grossAmount(), line.getDiscountPercent(), line.getDiscountAmount(),
                line.isDiscountConditional(), line.getTaxRate(), line.getTaxTreatment().name(),
                line.taxableBase(), line.getTaxAmount(), line.getLineTotal(), line.isEnabled());
    }
}
