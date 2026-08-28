package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.shared.pricing.PriceListValidity;
import com.vetsoftware.app.subscription.application.dto.PublishedCatalogItem;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionCommercialSnapshotPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.ContractPriceTier;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resuelve catálogo activo y tramos publicados sin aceptar dinero desde HTTP.
 *
 * <p>
 * <b>Ya no elige tramo.</b> Antes se quedaba con el de {@code tierMin} mas alto
 * que cubriera la cantidad -{@code max(tier_min)}- y el llamador multiplicaba
 * todo por el: la misma aritmetica plana que D-66 declara incorrecta. Ahora
 * devuelve el conjunto y el reparto acumulativo lo hace el dominio, donde se
 * puede comprobar.
 */
@Component("subscriptionJpaCommercialSnapshotPort")
public class JpaSubscriptionCommercialSnapshotPort implements SubscriptionCommercialSnapshotPort {

    private final EntityManager entityManager;

    public JpaSubscriptionCommercialSnapshotPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<PublishedCatalogItem> findPublishedItem(Long priceListId,
            BillingCycle billingCycle, Long catalogItemId, int quantity, LocalDate validOn) {
        List<Object[]> rows = entityManager.createQuery("""
                select ci.id, ci.code, ci.name, ci.itemType, ci.capacityUnit,
                       ci.minQuantity, ci.maxQuantity, ci.status,
                       cp.billingCycle, cp.tierMin, cp.tierMax, cp.includedQuantity,
                       cp.unitAmount, cp.taxRate, cp.taxTreatment,
                       pl.status, pl.validFrom, pl.validTo
                from CatalogItemJpaEntity ci, CatalogPriceJpaEntity cp, PriceListJpaEntity pl
                where ci.id = :catalogItemId and cp.catalogItemId = ci.id
                  and cp.priceListId = pl.id and pl.id = :priceListId
                  and ci.enabled = true and cp.enabled = true and pl.enabled = true
                order by cp.tierMin
                """, Object[].class).setParameter("catalogItemId", catalogItemId)
                .setParameter("priceListId", priceListId).getResultList().stream()
                .filter(row -> isApplicable(row, billingCycle, quantity, validOn)).toList();
        if (rows.isEmpty())
            return Optional.empty();
        List<ContractPriceTier> tiers = new ArrayList<>();
        for (Object[] row : rows) {
            tiers.add(new ContractPriceTier(((Number) row[9]).intValue(),
                    row[10] == null ? null : ((Number) row[10]).intValue(),
                    ((Number) row[11]).intValue(), TaxTreatment.valueOf(row[14].toString()),
                    (BigDecimal) row[12], (BigDecimal) row[13]));
        }
        Object[] head = rows.get(0);
        return Optional
                .of(new PublishedCatalogItem(((Number) head[0]).longValue(), head[1].toString(),
                        head[2].toString(), SubscriptionItemType.valueOf(head[3].toString()),
                        head[4] == null ? null : head[4].toString(), tiers));
    }

    /**
     * El filtro por tramo desaparecio a proposito: la cantidad ya no elige tramo,
     * lo reparte. Lo que si sigue acotando es el articulo -sus minimos y maximos-,
     * el ciclo, el estado y la VIGENCIA POR FECHA de la lista (D-73), que es lo que
     * impide cotizar hoy con una tarifa de 2025.
     *
     * <p>
     * <b>La comparacion de fechas ya no se escribe aqui.</b> Estaba copiada en
     * linea -{@code !validOn.isBefore(validFrom) && (validTo == null || ...)}- y
     * era la segunda copia viva del predicado que D-73 dejo en
     * {@link PriceListValidity}. Dos comparaciones que nada obliga a mover juntas
     * acaban divergiendo, y este es el sitio donde se decide si una linea se
     * factura: el dia que una tratase el extremo o el nulo de otra forma, el
     * sistema cotizaria con una regla y facturaria con otra.
     *
     * <p>
     * <b>El dia nulo se sigue filtrando aqui, y esa guarda se queda.</b>
     * {@link PriceListValidity#isEffectiveOn(LocalDate)} <em>lanza</em> sin fecha,
     * porque quien pregunta por una vigencia sin decir contra que dia tiene un
     * defecto. Este puerto no pregunta: filtra filas, y su contrato -fijado por
     * {@code SubscriptionOutboundPortsPersistenceIT.sinDiaDeReferencia}- es
     * devolver {@code Optional.empty()}, «ningun tramo aplica». Delegar el nulo en
     * el kernel convertiria un vacio en una excepcion que sube desde un adaptador
     * de lectura, y ademas lanzaria una vez por fila. Se descarta antes, y la
     * ventana se construye solo cuando hay dia contra el que compararla.
     */
    private static boolean isApplicable(Object[] row, BillingCycle billingCycle, int quantity,
            LocalDate validOn) {
        if (validOn == null)
            return false;
        int itemMin = ((Number) row[5]).intValue();
        Integer itemMax = row[6] == null ? null : ((Number) row[6]).intValue();
        return "ACTIVE".equals(row[7].toString()) && billingCycle.name().equals(row[8].toString())
                && "PUBLISHED".equals(row[15].toString())
                && new PriceListValidity((LocalDate) row[16], (LocalDate) row[17])
                        .isEffectiveOn(validOn)
                && quantity >= itemMin && (itemMax == null || quantity <= itemMax);
    }
}
