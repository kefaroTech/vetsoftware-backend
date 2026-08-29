package com.vetsoftware.app.quote.application.dto;

import com.vetsoftware.app.quote.domain.QuoteLine;
import com.vetsoftware.app.quote.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Un renglon de la vista previa, con el mismo desglose con el que se va a
 * facturar.
 *
 * <p>
 * <strong>Sale un renglon por tramo, no uno por articulo.</strong> Quince
 * usuarios con la escalera de la semilla salen como dos: ocho a 12.000 y cinco
 * a 9.000. Es lo que hace que el cliente pueda comprobar la cuenta en vez de
 * tener que creersela, y es la misma forma que tendra la oferta cuando la firme
 * (R-QUOTE-09).
 *
 * <p>
 * Sin id de articulo: la vista previa la pide un anonimo y habla por rotulos,
 * como el resto de la superficie publica.
 */
public record QuotePreviewLineDto(String code, String name, int contractedQuantity,
        int includedQuantity, int quantity, BigDecimal unitAmount, BigDecimal grossAmount,
        BigDecimal taxRate, TaxTreatment taxTreatment, BigDecimal taxAmount, BigDecimal lineTotal) {

    /** El renglon ya congelado por el mismo codigo que congela una oferta real. */
    public static QuotePreviewLineDto from(QuoteLine line) {
        return new QuotePreviewLineDto(line.getItemCode(), line.getItemName(),
                line.getContractedQuantity(), line.getIncludedQuantity(), line.getQuantity(),
                line.getUnitAmount(), line.grossAmount(), line.getTaxRate(), line.getTaxTreatment(),
                line.getTaxAmount(), line.getLineTotal());
    }
}
