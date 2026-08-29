package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.application.dto.PublicPlanComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPriceListDto;
import com.vetsoftware.app.pricelist.application.port.out.PublicPlanQueryPort;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * SQL nativo, y no los {@code JpaRepository} de al lado, por dos motivos
 * distintos y los dos deliberados.
 *
 * <p>
 * El primero es el que ya escribio {@link JpaCatalogItemQueryPort}: lo que esta
 * especificado como norma son la TABLA y sus COLUMNAS, no los nombres de campo
 * Java que elija el slice vecino. Aqui se leen ademas {@code bundle_components}
 * y {@code catalog_items}, que son de {@code catalogitem}: un JPQL contra sus
 * entidades ataria este archivo a una decision de modelado ajena que puede
 * cambiar sin que nadie lo note.
 *
 * <p>
 * El segundo es el que justifica el puerto entero: aqui se enumeran <b>una a
 * una</b> las columnas que el mundo puede ver. Cargar
 * {@code CatalogPriceJpaEntity} y proyectar despues deja la puerta abierta a
 * que un campo nuevo se cuele en la respuesta publica el dia que alguien lo
 * anada al agregado; un {@code SELECT} explicito no.
 *
 * <p>
 * <b>Nada de esto tiene {@code company_id}</b>: {@code price_lists},
 * {@code catalog_prices}, {@code catalog_items} y {@code bundle_components} son
 * catalogo global de plataforma, asi que no hay empresa que acotar. Solo lee.
 *
 * <p>
 * <b>Queda fuera de {@code ADAPTADOR_JPA_CON_RODAJA}</b>, que solo alcanza a
 * los {@code Jpa<Algo>Repository}. Su SQL necesita por tanto una rodaja escrita
 * a mano contra MySQL real —{@code PublicPlanQueryPortIT}— o no se ejecutara
 * nunca en el build, que es exactamente como sobrevivio meses el defecto de la
 * incidencia #196.
 */
@Component
public class JpaPublicPlanQueryPort implements PublicPlanQueryPort {

    /**
     * Sin filtro de fecha a proposito: la vigencia la decide
     * {@code PriceListValidity} sobre el reloj inyectado. Ver el puerto.
     */
    private static final String SQL_PRICE_LISTS = """
            SELECT id, currency, valid_from, valid_to
              FROM price_lists
             WHERE status = 'PUBLISHED'
               AND enabled = TRUE
             ORDER BY valid_from DESC, id DESC
            """;

    /**
     * Un plan es un paquete vendible con precio de entrada.
     *
     * <p>
     * Los dos {@code JOIN} son {@code LEFT} y el {@code WHERE} exige que al menos
     * uno case: un paquete tarifado solo en anual es legitimo y tiene que salir,
     * pero uno sin ningun precio en la tarifa vigente no es un plan que se pueda
     * publicar. {@code tier_min = 1} acota al <b>tramo de entrada</b>: la escalera
     * completa es la politica de descuento por volumen y no se publica.
     *
     * <p>
     * {@code COALESCE} prefiere el mensual para el cargo de implantacion, la tarifa
     * y el tratamiento fiscal: son los mismos en los dos ciclos en cualquier
     * catalogo sano, y si divergieran, el ciclo por defecto de la landing es el
     * mensual.
     */
    private static final String SQL_PLANS = """
            SELECT b.code,
                   b.name,
                   b.short_description,
                   pm.unit_amount,
                   pa.unit_amount,
                   COALESCE(pm.setup_amount, pa.setup_amount),
                   COALESCE(pm.tax_rate, pa.tax_rate),
                   COALESCE(pm.tax_treatment, pa.tax_treatment)
              FROM catalog_items b
              LEFT JOIN catalog_prices pm
                     ON pm.catalog_item_id = b.id
                    AND pm.price_list_id = :priceListId
                    AND pm.billing_cycle = 'MONTHLY'
                    AND pm.tier_min = 1
                    AND pm.enabled = TRUE
              LEFT JOIN catalog_prices pa
                     ON pa.catalog_item_id = b.id
                    AND pa.price_list_id = :priceListId
                    AND pa.billing_cycle = 'ANNUAL'
                    AND pa.tier_min = 1
                    AND pa.enabled = TRUE
             WHERE b.item_type = 'BUNDLE'
               AND b.status = 'ACTIVE'
               AND b.enabled = TRUE
               AND (pm.id IS NOT NULL OR pa.id IS NOT NULL)
             ORDER BY b.sort_order, b.id
            """;

    /**
     * Las lineas del paquete.
     *
     * <p>
     * {@code status = 'ACTIVE'} en el componente y no solo {@code enabled}: un
     * {@code DRAFT} es una idea a medias y un {@code DEPRECATED} ya no se vende;
     * anunciar cualquiera de los dos en una landing es prometer lo que no hay.
     *
     * <p>
     * {@code item_type IN ('MODULE','CAPACITY')} deja fuera los {@code ONE_TIME}
     * —implantacion, migracion, capacitacion: cargos negociados que no son precio
     * de lista— y los paquetes anidados.
     *
     * <p>
     * El {@code CASE} sobre {@code trial_eligibility} es lo que impide prometer una
     * prueba que nadie concedio. Proyecta un entero o nulo, nunca un literal
     * booleano ({@code PROYECCION_SIN_LITERAL_BOOLEANO}, incidencia #196).
     */
    private static final String SQL_COMPONENTS = """
            SELECT bi.code,
                   ci.code,
                   ci.name,
                   ci.capacity_unit,
                   bc.quantity,
                   CASE WHEN ci.trial_eligibility = 'ELIGIBLE'
                        THEN ci.default_trial_days END,
                   p.unit_amount
              FROM bundle_components bc
              JOIN catalog_items bi ON bi.id = bc.bundle_item_id
              JOIN catalog_items ci ON ci.id = bc.component_item_id
              LEFT JOIN catalog_prices p
                     ON p.catalog_item_id = ci.id
                    AND p.price_list_id = :priceListId
                    AND p.billing_cycle = 'MONTHLY'
                    AND p.tier_min = 1
                    AND p.enabled = TRUE
             WHERE bc.enabled = TRUE
               AND bi.enabled = TRUE
               AND bi.item_type = 'BUNDLE'
               AND bi.status = 'ACTIVE'
               AND ci.enabled = TRUE
               AND ci.status = 'ACTIVE'
               AND ci.item_type IN ('MODULE', 'CAPACITY')
             ORDER BY bi.code, ci.sort_order, ci.id
            """;

    private final EntityManager entityManager;

    public JpaPublicPlanQueryPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<PublicPriceListDto> findPublishedPriceLists() {
        Query query = entityManager.createNativeQuery(SQL_PRICE_LISTS);
        List<PublicPriceListDto> listas = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            listas.add(new PublicPriceListDto(asLong(columns[0]), asString(columns[1]),
                    asLocalDate(columns[2]), asLocalDate(columns[3])));
        }
        return List.copyOf(listas);
    }

    @Override
    public List<PublicPlanRowDto> findPlans(Long priceListId) {
        if (priceListId == null) {
            return List.of();
        }
        Query query = entityManager.createNativeQuery(SQL_PLANS).setParameter("priceListId",
                priceListId);
        List<PublicPlanRowDto> planes = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            planes.add(new PublicPlanRowDto(asString(columns[0]), asString(columns[1]),
                    asString(columns[2]), asAmount(columns[3]), asAmount(columns[4]),
                    asAmount(columns[5]), asAmount(columns[6]), asTaxTreatment(columns[7])));
        }
        return List.copyOf(planes);
    }

    @Override
    public List<PublicPlanComponentRowDto> findPlanComponents(Long priceListId) {
        if (priceListId == null) {
            return List.of();
        }
        Query query = entityManager.createNativeQuery(SQL_COMPONENTS).setParameter("priceListId",
                priceListId);
        List<PublicPlanComponentRowDto> lineas = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            lineas.add(new PublicPlanComponentRowDto(asString(columns[0]), asString(columns[1]),
                    asString(columns[2]), asString(columns[3]), asInt(columns[4]),
                    asInteger(columns[5]), asAmount(columns[6])));
        }
        return List.copyOf(lineas);
    }

    private static Long asLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static int asInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static Integer asInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static BigDecimal asAmount(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof BigDecimal decimal
                ? decimal
                : new BigDecimal(String.valueOf(value));
    }

    /**
     * El driver puede devolver la columna {@code DATE} como {@link LocalDate} o
     * como {@link Date} segun version y dialecto; una consulta nativa no declara
     * tipo, asi que se aceptan los dos en vez de confiar en cual toca hoy.
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

    private static TaxTreatment asTaxTreatment(Object value) {
        return value == null ? null : TaxTreatment.valueOf(String.valueOf(value));
    }
}
