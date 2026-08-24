package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.dto.SubscriptionItemSnapshot;
import com.vetsoftware.app.subscription.application.dto.SubscriptionQuoteSnapshot;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionQuoteSnapshotPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.CapacityUnit;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Proyección de quote acotada por empresa sin importar su dominio. */
@Component("subscriptionJpaQuoteSnapshotPort")
public class JpaSubscriptionQuoteSnapshotPort implements SubscriptionQuoteSnapshotPort {

    private final EntityManager entityManager;

    public JpaSubscriptionQuoteSnapshotPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<SubscriptionQuoteSnapshot> findByIdAndCompanyId(Long quoteId, Long companyId) {
        List<Object[]> headers = entityManager.createQuery("""
                select q.id, q.company.id, q.priceListId, q.billingCycle, q.status,
                       q.acceptedByEmail
                from QuoteJpaEntity q
                where q.id = :quoteId and q.company.id = :companyId and q.enabled = true
                """, Object[].class).setParameter("quoteId", quoteId)
                .setParameter("companyId", companyId).setMaxResults(1).getResultList();
        if (headers.isEmpty())
            return Optional.empty();
        Object[] header = headers.getFirst();
        List<SubscriptionItemSnapshot> items = entityManager.createQuery("""
                select l.catalogItemId, l.itemCode, l.itemName, l.itemType,
                       c.capacityUnit, l.includedQuantity, l.taxTreatment,
                       l.contractedQuantity, l.unitAmount, l.taxRate
                from QuoteJpaEntity q join q.lines l, CatalogItemJpaEntity c
                where q.id = :quoteId and q.company.id = :companyId
                  and l.catalogItemId = c.id and l.enabled = true
                order by l.lineNumber
                """, Object[].class).setParameter("quoteId", quoteId)
                .setParameter("companyId", companyId).getResultList().stream()
                .map(JpaSubscriptionQuoteSnapshotPort::toSnapshot).toList();
        return Optional.of(new SubscriptionQuoteSnapshot(number(header[0]), number(header[1]),
                number(header[2]), BillingCycle.valueOf(header[3].toString()),
                "ACCEPTED".equals(header[4].toString()), text(header[5]), items));
    }

    private static SubscriptionItemSnapshot toSnapshot(Object[] row) {
        return new SubscriptionItemSnapshot(number(row[0]), row[1].toString(), row[2].toString(),
                SubscriptionItemType.valueOf(row[3].toString()),
                row[4] == null ? null : CapacityUnit.valueOf(row[4].toString()),
                ((Number) row[5]).intValue(), TaxTreatment.valueOf(row[6].toString()),
                ((Number) row[7]).intValue(), (BigDecimal) row[8], (BigDecimal) row[9]);
    }

    private static Long number(Object value) {
        return ((Number) value).longValue();
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
