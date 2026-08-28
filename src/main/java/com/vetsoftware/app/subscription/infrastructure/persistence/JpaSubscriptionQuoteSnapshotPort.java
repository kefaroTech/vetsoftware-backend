package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.dto.SubscriptionItemSnapshot;
import com.vetsoftware.app.subscription.application.dto.SubscriptionQuoteSnapshot;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionQuoteSnapshotPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
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
                       l.contractedQuantity, l.unitAmount, l.taxRate,
                       l.tierMin, l.tierMax, l.discountPercent, l.discountAmount,
                       l.discountIsConditional, l.quantity
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

    /**
     * El renglon de la oferta ya viene partido por tramo (D-66) y con su descuento
     * marcado (D-86): aqui no se reparte ni se recalcula nada, se COPIA. Un
     * contrato que no diga exactamente lo que decia el papel que firmo el cliente
     * no vale nada, y cualquier cuenta que se rehiciera aqui podria dar otro
     * numero.
     *
     * <p>
     * <b>Lo incluido viaja SOLO en el renglon del primer tramo</b>, y esa es toda
     * la sutileza de esta traduccion. El renglon de la oferta guarda las TRES
     * cifras -contratada, incluida y cobrada- y repite la incluida en todos sus
     * tramos, porque es propiedad del articulo; la linea del contrato, en cambio,
     * guarda dos -{@code quantity} e {@code included_quantity}- y factura la resta.
     * Copiar la incluida en cada tramo la regalaria una vez por tramo, y copiar la
     * contratada como cantidad facturaria las quince unidades en CADA linea. Se
     * traduce a {@code cobradas + incluidas} en el primer tramo y a
     * {@code cobradas} en el resto: la suma de las lineas devuelve exactamente lo
     * contratado y cada una factura las unidades de su tramo.
     */
    private static SubscriptionItemSnapshot toSnapshot(Object[] row) {
        int tierMin = ((Number) row[10]).intValue();
        Integer tierMax = row[11] == null ? null : ((Number) row[11]).intValue();
        int cobradas = ((Number) row[15]).intValue();
        int incluidasDelArticulo = ((Number) row[5]).intValue();
        boolean primerTramo = tierMin == 1;
        int incluidas = primerTramo ? incluidasDelArticulo : 0;
        return new SubscriptionItemSnapshot(number(row[0]), row[1].toString(), row[2].toString(),
                SubscriptionItemType.valueOf(row[3].toString()),
                row[4] == null ? null : row[4].toString(), tierMin, tierMax, incluidas,
                TaxTreatment.valueOf(row[6].toString()), cobradas + incluidas, (BigDecimal) row[8],
                (BigDecimal) row[12], (BigDecimal) row[13], Boolean.TRUE.equals(row[14]),
                (BigDecimal) row[9]);
    }

    private static Long number(Object value) {
        return ((Number) value).longValue();
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
