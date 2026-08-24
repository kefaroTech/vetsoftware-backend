package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentTax;
import org.springframework.stereotype.Component;

/** Ida y vuelta entre la línea del desglose y su fila. */
@Component
public class SubscriptionBillingDocumentTaxJpaMapper {

    public SubscriptionBillingDocumentTaxJpaEntity toJpa(BillingDocumentTax tax) {
        SubscriptionBillingDocumentTaxJpaEntity entity = new SubscriptionBillingDocumentTaxJpaEntity();
        entity.setId(tax.id());
        entity.setCompanyId(tax.companyId());
        entity.setBillingDocumentId(tax.billingDocumentId());
        entity.setTaxTreatment(tax.taxTreatment());
        entity.setTaxRate(tax.taxRate());
        entity.setTaxableBase(tax.taxableBase());
        entity.setTaxAmount(tax.taxAmount());
        entity.setCreatedDate(tax.createdDate());
        return entity;
    }

    public BillingDocumentTax toDomain(SubscriptionBillingDocumentTaxJpaEntity entity) {
        return new BillingDocumentTax(entity.getId(), entity.getCompanyId(),
                entity.getBillingDocumentId(), entity.getTaxTreatment(), entity.getTaxRate(),
                entity.getTaxableBase(), entity.getTaxAmount(), entity.getCreatedDate());
    }
}
