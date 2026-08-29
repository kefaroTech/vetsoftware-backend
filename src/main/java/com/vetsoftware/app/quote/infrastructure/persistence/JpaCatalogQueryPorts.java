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
import java.time.LocalDate;
import java.util.ArrayList;
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

    /**
     * Una columna {@code DATE} de una consulta nativa. El driver puede entregarla
     * como {@link LocalDate} o como {@link java.sql.Date} segun la version, y el
     * nulo se conserva: {@code price_lists.valid_to} es nulable y ese nulo
     * significa «sin fecha de fin», no «sin dato».
     */
    private static LocalDate date(Object value) {
        if (value == null)
            return null;
        if (value instanceof LocalDate localDate)
            return localDate;
        if (value instanceof java.sql.Date sqlDate)
            return sqlDate.toLocalDate();
        if (value instanceof java.sql.Timestamp timestamp)
            return timestamp.toLocalDateTime().toLocalDate();
        return LocalDate.parse(String.valueOf(value));
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
         *
         * <p>
         * <b>El SELECT trae {@code valid_from} y {@code valid_to}, y es el arreglo de
         * D-73.</b> Con el filtro por estado a secas, esta consulta devolvia igual de
         * contenta la tarifa del ano pasado si nadie se acordo de archivarla: <i>hoy se
         * cotizaba con precios de 2025</i>, sin error, sin alarma y sin nada que mirar
         * en el documento firmado que dijera que la tarifa ya no regia. El camino del
         * contrato -{@code JpaSubscriptionCommercialSnapshotPort}- si comprobaba la
         * vigencia; este, que es por donde entra el dinero nuevo, no.
         *
         * <p>
         * <b>La ventana viaja, no se filtra.</b> Meter
         * {@code valid_from <= :hoy AND (valid_to IS NULL OR :hoy <= valid_to)} en el
         * WHERE seria una linea mas corta y perderia la unica informacion que importa
         * cuando falla: una lista caducada volveria como {@code Optional.empty()},
         * indistinguible de un id que no existe. Ademas obligaria a bajar el reloj
         * hasta el adaptador, donde la decision solo se puede probar levantando la base
         * de datos entera. Se devuelven las dos fechas y decide el caso de uso, que ya
         * tiene el {@code Clock} zonado.
         */
        @Override
        public Optional<PriceListRef> findPublishedById(Long priceListId) {
            Query query = entityManager.createNativeQuery("""
                    SELECT id, code, currency, valid_from, valid_to
                      FROM price_lists
                     WHERE id = :id
                       AND status = 'PUBLISHED'
                       AND enabled = TRUE
                    """).setParameter("id", priceListId);
            return singleRow(query).map(row -> new PriceListRef(id(row[0]), text(row[1]),
                    text(row[2]), date(row[3]), date(row[4])));
        }

        /**
         * Mismo {@code SELECT} y misma renuncia a filtrar por fecha que el de arriba,
         * sin el {@code WHERE id}. Lo consume la autocontratacion, que no recibe
         * {@code priceListId} porque elegir tarifa es elegir precio.
         *
         * <p>
         * El orden lo pone igualmente el caso de uso —de las vigentes gana la de
         * {@code valid_from} mas reciente—, pero se devuelve ya ordenado para que el
         * resultado no dependa del plan que elija el motor.
         */
        @Override
        public List<PriceListRef> findAllPublished() {
            Query query = entityManager.createNativeQuery("""
                    SELECT id, code, currency, valid_from, valid_to
                      FROM price_lists
                     WHERE status = 'PUBLISHED'
                       AND enabled = TRUE
                     ORDER BY valid_from DESC, id DESC
                    """);
            List<PriceListRef> listas = new ArrayList<>();
            for (Object row : query.getResultList()) {
                Object[] columns = (Object[]) row;
                listas.add(new PriceListRef(id(columns[0]), text(columns[1]), text(columns[2]),
                        date(columns[3]), date(columns[4])));
            }
            return List.copyOf(listas);
        }
    }

    /** Devuelve TODOS los tramos de precio del articulo en esa tarifa y ciclo. */
    @Component
    public static class JpaCatalogPriceQueryPort implements CatalogPriceQueryPort {

        private final EntityManager entityManager;

        public JpaCatalogPriceQueryPort(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        /**
         * <b>Sin recorte por cantidad, y es el arreglo entero de D-66.</b> Esta
         * consulta tenia un {@code tier_min <= :quantity ... ORDER BY tier_min DESC}
         * con {@code LIMIT 1} y devolvia UN tramo -el mas alto que cubriera la
         * cantidad-, que el servicio multiplicaba por todas las unidades. Con "las
         * unidades extra 1 a 8 a 12.000 y de la 9 en adelante a 9.000", trece unidades
         * salian a 117.000 en vez de a 141.000: veinticuatro mil por cliente y mes, y
         * ni un error ni una alarma en ningun sitio.
         *
         * <p>
         * Los tramos son un punado de filas por articulo y ciclo -siete articulos
         * escalonados en todo el catalogo-, asi que traerlos todos y repartir en el
         * dominio no cuesta nada y deja la cuenta donde se puede comprobar en cada
         * lectura.
         *
         * <p>
         * El {@code ORDER BY tier_min} deja de ser el que decide el precio y pasa a ser
         * solo orden estable de salida: quien decide es {@code TieredPrice}, que ademas
         * exige que el primer tramo arranque en uno.
         */
        @Override
        public List<CatalogPriceRef> findAllTiers(Long priceListId, Long catalogItemId,
                BillingCycle billingCycle) {
            Query query = entityManager.createNativeQuery("""
                    SELECT unit_amount, tax_rate, tax_treatment, included_quantity,
                           tier_min, tier_max
                      FROM catalog_prices
                     WHERE price_list_id   = :priceListId
                       AND catalog_item_id = :catalogItemId
                       AND billing_cycle   = :billingCycle
                       AND enabled = TRUE
                     ORDER BY tier_min
                    """).setParameter("priceListId", priceListId)
                    .setParameter("catalogItemId", catalogItemId)
                    .setParameter("billingCycle", billingCycle.name());
            List<CatalogPriceRef> tiers = new ArrayList<>();
            for (Object row : query.getResultList()) {
                Object[] columns = (Object[]) row;
                tiers.add(new CatalogPriceRef(amount(columns[0]), amount(columns[1]),
                        TaxTreatment.valueOf(text(columns[2])), count(columns[3]),
                        count(columns[4]), columns[5] == null ? null : count(columns[5])));
            }
            return List.copyOf(tiers);
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
