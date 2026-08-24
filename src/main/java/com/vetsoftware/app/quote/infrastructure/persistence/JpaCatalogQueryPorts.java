package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.quote.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.quote.application.port.out.CatalogPriceQueryPort;
import com.vetsoftware.app.quote.application.port.out.ConfiguratorQuestionQueryPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CatalogItemRef;
import com.vetsoftware.app.quote.domain.CatalogPriceRef;
import com.vetsoftware.app.quote.domain.ConfiguratorQuestionRef;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.QuoteItemType;
import com.vetsoftware.app.quote.domain.TaxTreatment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Los cuatro adaptadores de lectura contra los slices vecinos del catalogo:
 * {@code catalog_items}, {@code price_lists}, {@code catalog_prices} y
 * {@code configurator_questions}.
 *
 * <p>
 * <b>Por que SQL nativo y no un {@code XxxJpaRepository} de la otra
 * feature.</b> Lo que esta especificado como norma en
 * {@code suscripciones-tablas.md} son las TABLAS y sus COLUMNAS, no los nombres
 * de campo Java que elija cada slice. Un JPQL contra
 * {@code CatalogPriceJpaEntity.priceList.id} ata este archivo a una decision de
 * modelado ajena que puede cambiar sin que nadie lo note aqui; el SQL nativo se
 * ata al esquema, que es el contrato firmado y el mismo que valida
 * {@code ddl-auto: validate} al arrancar. La eleccion es deliberada y no un
 * atajo.
 *
 * <p>
 * <b>Y todas son consultas de LECTURA.</b> Ninguna escribe: la regla
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} mira
 * {@code UPDATE}/{@code DELETE}, y ninguna de estas tablas lleva
 * {@code company_id} -son catalogo global de plataforma-, asi que no hay
 * empresa que acotar. Se dice aqui por escrito para que la proxima auditoria no
 * tenga que deducirlo.
 *
 * <p>
 * Las cuatro clases viven en un archivo con el mismo nombre que la clase
 * envolvente porque son cuatro adaptadores triviales del mismo vecindario; cada
 * una es un {@code @Component} independiente.
 */
public final class JpaCatalogQueryPorts {

    private JpaCatalogQueryPorts() {
    }

    private static Optional<Object[]> singleRow(Query query) {
        List<?> rows = query.setMaxResults(1).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of((Object[]) rows.get(0));
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long id(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static BigDecimal amount(Object value) {
        return value == null ? null : new BigDecimal(String.valueOf(value));
    }

    private static int count(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    /** Lee el articulo del catalogo, solo si esta ACTIVE y habilitado. */
    @Component
    public static class JpaCatalogItemQueryPort implements CatalogItemQueryPort {

        private final EntityManager entityManager;

        public JpaCatalogItemQueryPort(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        /**
         * El filtro por {@code status = 'ACTIVE'} no es cosmetico: un articulo en DRAFT
         * todavia se esta redactando y uno DEPRECATED se retiro de la venta. Congelar
         * cualquiera de los dos en un documento con valor legal es vender algo que la
         * plataforma no ofrece.
         */
        @Override
        public Optional<CatalogItemRef> findActiveById(Long catalogItemId) {
            Query query = entityManager.createNativeQuery("""
                    SELECT id, code, name, item_type
                      FROM catalog_items
                     WHERE id = :id
                       AND status = 'ACTIVE'
                       AND enabled = TRUE
                    """).setParameter("id", catalogItemId);
            return singleRow(query).map(row -> new CatalogItemRef(id(row[0]), text(row[1]),
                    text(row[2]), QuoteItemType.valueOf(text(row[3]))));
        }
    }

    /** Lee la tarifa, solo si esta PUBLISHED. */
    @Component
    public static class JpaPriceListQueryPort implements PriceListQueryPort {

        private final EntityManager entityManager;

        public JpaPriceListQueryPort(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        /**
         * Cotizar contra una lista en DRAFT congelaria en un documento con valor legal
         * unos precios que todavia se estaban editando -y que la regla R9 permite
         * cambiar mientras siga en borrador-.
         */
        @Override
        public Optional<PriceListRef> findPublishedById(Long priceListId) {
            Query query = entityManager.createNativeQuery("""
                    SELECT id, code, currency
                      FROM price_lists
                     WHERE id = :id
                       AND status = 'PUBLISHED'
                       AND enabled = TRUE
                    """).setParameter("id", priceListId);
            return singleRow(query)
                    .map(row -> new PriceListRef(id(row[0]), text(row[1]), text(row[2])));
        }
    }

    /** Resuelve el tramo de precio aplicable a la cantidad pedida. */
    @Component
    public static class JpaCatalogPriceQueryPort implements CatalogPriceQueryPort {

        private final EntityManager entityManager;

        public JpaCatalogPriceQueryPort(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        /**
         * Precio por tramos: se toma el tramo cuyo {@code tier_min} es el mas alto de
         * los que no superan la cantidad, con {@code tier_max} nulo -"de ahi en
         * adelante"- o mayor o igual que ella.
         *
         * <p>
         * El {@code ORDER BY tier_min DESC} es lo que hace que "los usuarios 3 a 10 a
         * 12.000 y del 11 en adelante a 9.000" devuelva 9.000 para 15 usuarios y no
         * 12.000. Sin el, el resultado depende del orden fisico de las filas.
         */
        @Override
        public Optional<CatalogPriceRef> findApplicable(Long priceListId, Long catalogItemId,
                BillingCycle billingCycle, int quantity) {
            Query query = entityManager.createNativeQuery("""
                    SELECT unit_amount, tax_rate, tax_treatment, included_quantity
                      FROM catalog_prices
                     WHERE price_list_id  = :priceListId
                       AND catalog_item_id = :catalogItemId
                       AND billing_cycle   = :billingCycle
                       AND tier_min       <= :quantity
                       AND (tier_max IS NULL OR tier_max >= :quantity)
                       AND enabled = TRUE
                     ORDER BY tier_min DESC
                    """).setParameter("priceListId", priceListId)
                    .setParameter("catalogItemId", catalogItemId)
                    .setParameter("billingCycle", billingCycle.name())
                    .setParameter("quantity", quantity);
            return singleRow(query).map(row -> new CatalogPriceRef(amount(row[0]), amount(row[1]),
                    TaxTreatment.valueOf(text(row[2])), count(row[3])));
        }
    }

    /** Lee la pregunta del configurador solo para copiar su codigo. */
    @Component
    public static class JpaConfiguratorQuestionQueryPort implements ConfiguratorQuestionQueryPort {

        private final EntityManager entityManager;

        public JpaConfiguratorQuestionQueryPort(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Override
        public Optional<ConfiguratorQuestionRef> findById(Long questionId) {
            Query query = entityManager.createNativeQuery("""
                    SELECT id, code
                      FROM configurator_questions
                     WHERE id = :id
                       AND enabled = TRUE
                    """).setParameter("id", questionId);
            return singleRow(query)
                    .map(row -> new ConfiguratorQuestionRef(id(row[0]), text(row[1])));
        }
    }
}
