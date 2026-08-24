package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.dto.SubscriptionItemSnapshot;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionCommercialSnapshotPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.CapacityUnit;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Resuelve catálogo activo y tramo publicado sin aceptar dinero desde HTTP. */
@Component("subscriptionJpaCommercialSnapshotPort")
public class JpaSubscriptionCommercialSnapshotPort implements SubscriptionCommercialSnapshotPort {

    private final EntityManager entityManager;

    public JpaSubscriptionCommercialSnapshotPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<SubscriptionItemSnapshot> findPublishedItem(Long priceListId,
            BillingCycle billingCycle, Long catalogItemId, int quantity, LocalDate validOn) {
        return entityManager.createQuery("""
                select ci.id, ci.code, ci.name, ci.itemType, ci.capacityUnit,
                       ci.minQuantity, ci.maxQuantity, ci.status,
                       cp.billingCycle, cp.tierMin, cp.tierMax, cp.includedQuantity,
                       cp.unitAmount, cp.taxRate, cp.taxTreatment,
                       pl.status, pl.validFrom, pl.validTo
                from CatalogItemJpaEntity ci, CatalogPriceJpaEntity cp, PriceListJpaEntity pl
                where ci.id = :catalogItemId and cp.catalogItemId = ci.id
                  and cp.priceListId = pl.id and pl.id = :priceListId
                  and ci.enabled = true and cp.enabled = true and pl.enabled = true
                """, Object[].class).setParameter("catalogItemId", catalogItemId)
                .setParameter("priceListId", priceListId).getResultList().stream()
                .filter(row -> isApplicable(row, billingCycle, quantity, validOn))
                .max(Comparator.comparingInt(row -> ((Number) row[9]).intValue()))
                .map(row -> toSnapshot(row, quantity));
    }

    private static boolean isApplicable(Object[] row, BillingCycle billingCycle, int quantity,
            LocalDate validOn) {
        int itemMin = ((Number) row[5]).intValue();
        Integer itemMax = row[6] == null ? null : ((Number) row[6]).intValue();
        int tierMin = ((Number) row[9]).intValue();
        Integer tierMax = row[10] == null ? null : ((Number) row[10]).intValue();
        LocalDate validFrom = (LocalDate) row[16];
        LocalDate validTo = (LocalDate) row[17];
        return "ACTIVE".equals(row[7].toString()) && billingCycle.name().equals(row[8].toString())
                && "PUBLISHED".equals(row[15].toString()) && validOn != null
                && !validOn.isBefore(validFrom) && (validTo == null || !validOn.isAfter(validTo))
                && quantity >= itemMin && (itemMax == null || quantity <= itemMax)
                && quantity >= tierMin && (tierMax == null || quantity <= tierMax);
    }

    private static SubscriptionItemSnapshot toSnapshot(Object[] row, int quantity) {
        return new SubscriptionItemSnapshot(((Number) row[0]).longValue(), row[1].toString(),
                row[2].toString(), SubscriptionItemType.valueOf(row[3].toString()),
                row[4] == null ? null : CapacityUnit.valueOf(row[4].toString()),
                ((Number) row[11]).intValue(), TaxTreatment.valueOf(row[14].toString()), quantity,
                (BigDecimal) row[12], (BigDecimal) row[13]);
    }
}
