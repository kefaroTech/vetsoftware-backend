package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.domain.ProrationBasis;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import org.springframework.stereotype.Component;

/** El único sitio que conoce a la vez el cargo de dominio y su fila. */
@Component
public class SubscriptionChargeJpaMapper {

    public SubscriptionChargeJpaEntity toJpa(SubscriptionCharge charge) {
        SubscriptionChargeJpaEntity entity = new SubscriptionChargeJpaEntity();
        entity.setId(charge.getId());
        entity.setCompanyId(charge.getCompanyId());
        entity.setSubscriptionId(charge.getSubscriptionId());
        entity.setSubscriptionItemId(charge.getSubscriptionItemId());
        entity.setChargeType(charge.getChargeType());
        entity.setDescription(charge.getDescription());
        entity.setServicePeriodStart(charge.getServicePeriod().start());
        entity.setServicePeriodEnd(charge.getServicePeriod().end());
        entity.setQuantity(charge.getQuantity());
        entity.setUnitAmount(charge.getUnitAmount());
        entity.setSubtotalAmount(charge.getSubtotalAmount());
        entity.setTaxRate(charge.getTaxRate());
        entity.setTaxTreatment(charge.getTaxTreatment());
        ProrationBasis proration = charge.getProration();
        entity.setProrationDays(proration == null ? null : proration.prorationDays());
        entity.setPeriodDays(proration == null ? null : proration.periodDays());
        entity.setStatus(charge.getStatus());
        entity.setAmendmentId(charge.getAmendmentId());
        entity.setBillingDocumentId(charge.getBillingDocumentId());
        entity.setVoidsChargeId(charge.getVoidsChargeId());
        entity.setCreatedDate(charge.getCreatedDate());
        return entity;
    }

    public SubscriptionCharge toDomain(SubscriptionChargeJpaEntity entity) {
        return new SubscriptionCharge(entity.getId(), entity.getCompanyId(),
                entity.getSubscriptionId(), entity.getSubscriptionItemId(), entity.getChargeType(),
                entity.getDescription(),
                new ServicePeriod(entity.getServicePeriodStart(), entity.getServicePeriodEnd()),
                entity.getQuantity(), entity.getUnitAmount(), entity.getSubtotalAmount(),
                entity.getTaxRate(), entity.getTaxTreatment(),
                ProrationBasis.of(entity.getProrationDays(), entity.getPeriodDays()),
                entity.getStatus(), entity.getAmendmentId(), entity.getBillingDocumentId(),
                entity.getVoidsChargeId(), entity.getCreatedDate());
    }
}
