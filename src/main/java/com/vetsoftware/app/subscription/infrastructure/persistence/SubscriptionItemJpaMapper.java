package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/** Unico sitio que conoce a la vez la linea de dominio y su entidad JPA. */
@Component
public class SubscriptionItemJpaMapper {

    private final Clock clock;

    public SubscriptionItemJpaMapper(Clock clock) {
        this.clock = clock;
    }

    public SubscriptionItemJpaEntity toJpa(SubscriptionItem item, CompanyJpaEntity company,
            SubscriptionJpaEntity subscription) {
        SubscriptionItemJpaEntity entity = new SubscriptionItemJpaEntity();
        entity.setId(item.getId());
        entity.setCompany(company);
        entity.setSubscription(subscription);
        entity.setCatalogItemId(item.getCatalogItemId());
        entity.setItemCode(item.getItemCode());
        entity.setItemName(item.getItemName());
        entity.setItemType(item.getItemType());
        entity.setCapacityUnit(item.getCapacityUnit());
        // D-66: el tramo deja de ser columna muerta. Existia desde el 244 y no lo
        // escribia nadie, porque el unico camino de precio devolvia un tramo solo.
        entity.setTierMin(item.getTierMin());
        entity.setTierMax(item.getTierMax());
        entity.setIncludedQuantity(item.getIncludedQuantity());
        entity.setTaxTreatment(item.getTaxTreatment());
        entity.setQuantity(item.getQuantity());
        entity.setUnitAmount(item.getUnitAmount());
        entity.setDiscountPercent(item.getDiscountPercent());
        entity.setDiscountAmount(item.getDiscountAmount());
        entity.setDiscountIsConditional(item.isDiscountConditional());
        entity.setTaxRate(item.getTaxRate());
        entity.setEffectiveFrom(item.getPeriod().from());
        entity.setEffectiveTo(item.getPeriod().to());
        entity.setOrigin(item.getOrigin());
        entity.setCreatedAmendmentId(item.getCreatedAmendmentId());
        entity.setEndedAmendmentId(item.getEndedAmendmentId());
        entity.setCreatedDate(
                item.getCreatedDate() == null ? LocalDateTime.now(clock) : item.getCreatedDate());
        entity.setVersion(item.getVersion());
        entity.setEnabled(item.isEnabled());
        return entity;
    }

    public SubscriptionItem toDomain(SubscriptionItemJpaEntity entity) {
        return new SubscriptionItem(entity.getId(), entity.getCompany().getId(),
                entity.getSubscription().getId(), entity.getCatalogItemId(), entity.getItemCode(),
                entity.getItemName(), entity.getItemType(), entity.getCapacityUnit(),
                entity.getTierMin(), entity.getTierMax(), entity.getIncludedQuantity(),
                entity.getTaxTreatment(), entity.getQuantity(), entity.getUnitAmount(),
                entity.getDiscountPercent(), entity.getDiscountAmount(),
                entity.isDiscountIsConditional(), entity.getTaxRate(),
                new EffectivePeriod(entity.getEffectiveFrom(), entity.getEffectiveTo()),
                entity.getOrigin(), entity.getCreatedAmendmentId(), entity.getEndedAmendmentId(),
                entity.getCreatedDate(), entity.getVersion(), entity.isEnabled());
    }
}
