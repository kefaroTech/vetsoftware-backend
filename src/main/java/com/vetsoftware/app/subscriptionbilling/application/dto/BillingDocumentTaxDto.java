package com.vetsoftware.app.subscriptionbilling.application.dto;

import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentTax;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Una línea del desglose fiscal.
 *
 * <p>
 * {@code taxTreatment} viaja siempre junto a {@code taxRate}: exento y excluido
 * comparten tarifa cero y no son lo mismo, así que un desglose que solo
 * mostrara la tarifa sería indistinguible entre los dos.
 */
public record BillingDocumentTaxDto(Long id, TaxTreatment taxTreatment, BigDecimal taxRate,
        BigDecimal taxableBase, BigDecimal taxAmount) {

    public static BillingDocumentTaxDto from(BillingDocumentTax tax) {
        return new BillingDocumentTaxDto(tax.id(), tax.taxTreatment(), tax.taxRate(),
                tax.taxableBase(), tax.taxAmount());
    }
}
