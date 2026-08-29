package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.application.port.out.CapacityCeilingQueryPort;
import com.vetsoftware.app.configurator.domain.BillingCycle;
import com.vetsoftware.app.configurator.domain.PublishedPriceListRef;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * SQL nativo, por lo mismo que los demas adaptadores de este vecindario: lo
 * especificado como norma son la TABLA y sus COLUMNAS, no los nombres de campo
 * Java que elijan {@code catalogitem} ni {@code pricelist}.
 *
 * <p>
 * <strong>El ciclo es un parametro, no una constante.</strong>
 * {@code included_quantity} es columna de la fila de precio y hay una fila por
 * ciclo, asi que el techo puede diferir entre mensual y anual. Estuvo clavado
 * en {@code MONTHLY} mientras el command no traia ciclo, y era una bomba
 * silenciosa: el dia que un catalogo separase los dos valores, una cotizacion
 * anual se habria descontado con el techo mensual y las dos cifras habrian
 * seguido siendo plausibles. Ahora lo elige quien pregunta.
 */
@Component
public class JpaCapacityCeilingQueryPort implements CapacityCeilingQueryPort {

    private static final String SQL_PRICE_LISTS = """
            SELECT id, valid_from, valid_to
              FROM price_lists
             WHERE status = 'PUBLISHED'
               AND enabled = TRUE
             ORDER BY valid_from DESC, id DESC
            """;

    /**
     * El techo de cada eje: lo que trae el tramo de entrada del articulo del
     * nucleo, mas la cantidad con la que el contrato inicial lo concede.
     *
     * <p>
     * {@code is_core = TRUE} se usa aqui como <strong>predicado de
     * conjunto</strong> —«forma parte del minimo estructural»—, que es lo que la
     * columna significa y como la lee {@code findInitialCapacityTemplates}.
     * Confundir «el articulo CORE» con «el conjunto del nucleo» es lo que hizo
     * nacer empresas sin una sola capacidad (#490).
     */
    private static final String SQL_CEILINGS = """
            SELECT ci.capacity_unit,
                   cp.included_quantity + ci.min_quantity
              FROM catalog_items ci
              JOIN catalog_prices cp
                   ON  cp.catalog_item_id = ci.id
                   AND cp.price_list_id   = :priceListId
                   AND cp.billing_cycle   = :billingCycle
                   AND cp.tier_min        = 1
                   AND cp.enabled         = TRUE
             WHERE ci.item_type     = 'CAPACITY'
               AND ci.is_core       = TRUE
               AND ci.capacity_unit IS NOT NULL
               AND ci.status        = 'ACTIVE'
               AND ci.enabled       = TRUE
             ORDER BY ci.capacity_unit
            """;

    private final EntityManager entityManager;

    public JpaCapacityCeilingQueryPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<PublishedPriceListRef> findPublishedPriceLists() {
        Query query = entityManager.createNativeQuery(SQL_PRICE_LISTS);
        List<PublishedPriceListRef> listas = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            listas.add(new PublishedPriceListRef(((Number) columns[0]).longValue(),
                    asLocalDate(columns[1]), asLocalDate(columns[2])));
        }
        return List.copyOf(listas);
    }

    @Override
    public Map<String, Integer> findStructuralCeilingsByAxis(Long priceListId,
            BillingCycle billingCycle) {
        if (priceListId == null || billingCycle == null) {
            return Map.of();
        }
        Query query = entityManager.createNativeQuery(SQL_CEILINGS)
                .setParameter("priceListId", priceListId)
                .setParameter("billingCycle", billingCycle.name());
        Map<String, Integer> techos = new LinkedHashMap<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            techos.put(String.valueOf(columns[0]), ((Number) columns[1]).intValue());
        }
        return Map.copyOf(techos);
    }

    /**
     * El driver puede entregar una columna {@code DATE} como {@link LocalDate} o
     * como {@link Date} segun version y dialecto; una consulta nativa no declara
     * tipo. El nulo se conserva: {@code valid_to} nulable significa «sin fecha de
     * fin», no «sin dato».
     */
    private static LocalDate asLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
