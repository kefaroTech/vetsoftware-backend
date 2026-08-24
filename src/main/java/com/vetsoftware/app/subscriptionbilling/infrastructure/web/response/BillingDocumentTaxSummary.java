package com.vetsoftware.app.subscriptionbilling.infrastructure.web.response;

import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentTaxDto;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;

/**
 * Una línea del desglose fiscal, tal como sale por HTTP.
 *
 * <p>
 * {@code taxTreatment} sale siempre junto a {@code taxRate}: sin él, un exento
 * y un excluido se verían iguales —los dos con tarifa cero— y no lo son.
 */
public record BillingDocumentTaxSummary(Long id, TaxTreatment taxTreatment, BigDecimal taxRate,
        BigDecimal taxableBase, BigDecimal taxAmount) {

    public static BillingDocumentTaxSummary from(BillingDocumentTaxDto dto) {
        return new BillingDocumentTaxSummary(dto.id(), dto.taxTreatment(), dto.taxRate(),
                dto.taxableBase(), dto.taxAmount());
    }
}
