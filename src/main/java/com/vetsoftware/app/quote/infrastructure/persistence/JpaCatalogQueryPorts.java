package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.quote.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.quote.application.port.out.CatalogPriceQueryPort;
import com.vetsoftware.app.quote.application.port.out.ConfiguratorQuestionQueryPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.PublishedCatalogItemQueryPort;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Los cinco adaptadores de lectura contra los slices vecinos del catalogo:
 * {@code catalog_items}, {@code price_lists}, {@code catalog_prices},
 * {@code bundle_components} y {@code configurator_questions}.
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
 * Las cinco clases viven en un archivo con el mismo nombre que la clase
 * envolvente porque son adaptadores triviales del mismo vecindario; cada una es
 * un {@code @Component} independiente.
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

    /**
     * Traduce el rotulo publico de un articulo a su id, y <b>solo dentro del
     * conjunto que publica {@code GET /plans}</b>.
     *
     * <p>
     * Es el adaptador mas delicado del archivo, porque es el unico que un tenant
     * alcanza con un valor que el elige. Los demas reciben ids que ya paso
     * {@code SYSTEM}.
     */
    @Component
    public static class JpaPublishedCatalogItemQueryPort implements PublishedCatalogItemQueryPort {

        /**
         * <b>Este WHERE es el gate, y cada rama replica un predicado de
         * {@code JpaPublicPlanQueryPort}</b> —el que alimenta {@code GET /plans}—, no
         * uno parecido:
         *
         * <ul>
         * <li><b>La rama del paquete</b> es la de {@code SQL_PLANS}: {@code BUNDLE}
         * {@code ACTIVE} y habilitado.</li>
         * <li><b>La rama del componente</b> es la de {@code SQL_COMPONENTS}:
         * {@code MODULE} o {@code CAPACITY} {@code ACTIVE} y habilitado, colgando por
         * {@code bundle_components} de un paquete que a su vez esta publicado. Deja
         * fuera los {@code ONE_TIME} —implantacion, migracion, capacitacion: cargos
         * negociados que la portada no anuncia— y los articulos activos que no forman
         * parte de ningun plan, que son catalogo interno.</li>
         * <li><b>El {@code JOIN} con {@code catalog_prices}</b> exige precio de entrada
         * ({@code tier_min = 1}) <em>en la tarifa y el ciclo con los que se esta
         * cotizando</em>. Es lo que {@code SQL_PLANS} exige al paquete, aplicado
         * tambien al componente, y tiene una segunda utilidad: sin el, un articulo
         * publicado pero sin tarifar en el ciclo pedido llegaria hasta
         * {@code CreateQuoteService} y saldria por el error «No price for catalog item
         * 42 in price list…», <b>que devuelve el id</b> y reintroduce por la puerta de
         * atras justo lo que este puerto existe para no publicar.</li>
         * </ul>
         *
         * <p>
         * <b>Sin literales booleanos en la proyeccion</b>
         * ({@code PROYECCION_SIN_LITERAL_BOOLEANO}, incidencia #196): el {@code SELECT}
         * devuelve una sola columna, {@code ci.id}. Los {@code = TRUE} viven en el
         * {@code WHERE} y en los {@code JOIN}, como en los otros cuatro adaptadores de
         * este archivo.
         *
         * <p>
         * <b>Una fila como mucho, y no por el {@code LIMIT}.</b>
         * {@code uq_catalog_items_code} (changeset 229) es {@code UNIQUE} global sobre
         * {@code catalog_items.code}; el {@code setMaxResults(1)} de
         * {@link JpaCatalogQueryPorts#singleRow(Query)} es la red del patron del
         * archivo, no el criterio de desempate. No hay criterio de desempate porque no
         * puede haber empate.
         */
        private static final String SQL_PUBLISHED_ID_BY_CODE = """
                SELECT ci.id
                  FROM catalog_items ci
                  JOIN catalog_prices p
                       ON p.catalog_item_id = ci.id
                      AND p.price_list_id   = :priceListId
                      AND p.billing_cycle   = :billingCycle
                      AND p.tier_min        = 1
                      AND p.enabled = TRUE
                 WHERE ci.code = :code
                   AND ci.status = 'ACTIVE'
                   AND ci.enabled = TRUE
                   AND (ci.item_type = 'BUNDLE'
                        OR (ci.item_type IN ('MODULE', 'CAPACITY')
                            AND EXISTS (SELECT 1
                                          FROM bundle_components bc
                                          JOIN catalog_items b ON b.id = bc.bundle_item_id
                                         WHERE bc.component_item_id = ci.id
                                           AND bc.enabled = TRUE
                                           AND b.enabled = TRUE
                                           AND b.item_type = 'BUNDLE'
                                           AND b.status = 'ACTIVE')))
                """;

        /**
         * <b>La composicion de los paquetes nombrados, por rotulo.</b> Es el mismo
         * grafo que publica {@code GET /catalog} en
         * {@code PublicCatalogPackResponse.componentCodes}, con los mismos filtros
         * —paquete {@code BUNDLE} {@code ACTIVE} y habilitado, componente
         * {@code ACTIVE} y habilitado, puente habilitado—, para que el front pueda
         * evitar la cesta que este SQL sirve para rechazar en vez de provocarla.
         *
         * <p>
         * <b>Sin filtro por precio en el componente</b>, y es deliberado: la pregunta
         * no es «se puede comprar esta pieza suelta» sino «viene dentro de este
         * paquete». Una pieza sin tarifar suelta sigue estando dentro y seguiria
         * cobrandose dos veces si se colara; filtrarla dejaria fuera de la comprobacion
         * justo al caso que nadie mira.
         *
         * <p>
         * {@code DISTINCT} porque dos paquetes de la misma cesta pueden compartir pieza
         * —{@code CORE} esta en los tres— y al que llama solo le importa el conjunto.
         */
        private static final String SQL_COMPONENT_CODES_OF_BUNDLES = """
                SELECT DISTINCT ci.code
                  FROM bundle_components bc
                  JOIN catalog_items b  ON b.id  = bc.bundle_item_id
                  JOIN catalog_items ci ON ci.id = bc.component_item_id
                 WHERE b.code IN (:codes)
                   AND bc.enabled = TRUE
                   AND b.enabled = TRUE
                   AND b.item_type = 'BUNDLE'
                   AND b.status = 'ACTIVE'
                   AND ci.enabled = TRUE
                   AND ci.status = 'ACTIVE'
                 ORDER BY ci.code
                """;

        private final EntityManager entityManager;

        public JpaPublishedCatalogItemQueryPort(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        /**
         * Un {@code code} nulo o en blanco no llega a la base: no puede casar con
         * ninguna fila y el resultado seria el mismo, pero gastando una consulta que un
         * cliente puede repetir a voluntad.
         */
        @Override
        public Optional<Long> findPublishedIdByCode(String code, Long priceListId,
                BillingCycle billingCycle) {
            if (code == null || code.isBlank() || priceListId == null || billingCycle == null) {
                return Optional.empty();
            }
            Query query = entityManager.createNativeQuery(SQL_PUBLISHED_ID_BY_CODE)
                    .setParameter("code", code).setParameter("priceListId", priceListId)
                    .setParameter("billingCycle", billingCycle.name());
            List<?> rows = query.setMaxResults(1).getResultList();
            return rows.isEmpty() ? Optional.empty() : Optional.ofNullable(id(rows.get(0)));
        }

        /**
         * Una lista vacia no llega a la base: un {@code IN ()} es error de sintaxis en
         * MySQL, y el resultado seria vacio de todos modos.
         */
        @Override
        public List<String> findComponentCodesOfBundles(Collection<String> codes) {
            if (codes == null || codes.isEmpty()) {
                return List.of();
            }
            Query query = entityManager.createNativeQuery(SQL_COMPONENT_CODES_OF_BUNDLES)
                    .setParameter("codes", codes);
            List<String> componentes = new ArrayList<>();
            for (Object row : query.getResultList()) {
                componentes.add(text(row));
            }
            return List.copyOf(componentes);
        }

        /**
         * <b>Lo que la cesta necesita y no trae.</b>
         *
         * <p>
         * El {@code NOT IN} es el punto: la cobertura es la union de los rotulos
         * pedidos <em>y</em> los componentes de los paquetes pedidos. Sin esa segunda
         * mitad, comprar {@code PACK_FULL} —que trae dentro Facturacion Electronica y
         * Caja— saldria rechazado por no nombrar Caja en la peticion.
         *
         * <p>
         * Los dos extremos del arco tienen que estar vivos: un requisito que apunte a
         * un articulo retirado no se puede satisfacer, y exigirlo dejaria la cesta
         * imposible de completar.
         */
        private static final String SQL_MISSING_REQUIREMENTS = """
                SELECT DISTINCT req.code
                  FROM catalog_items ci
                  JOIN catalog_item_dependencies d
                       ON  d.catalog_item_id = ci.id
                       AND d.relation_type   = 'REQUIRES'
                       AND d.enabled         = TRUE
                  JOIN catalog_items req
                       ON  req.id     = d.related_item_id
                       AND req.status = 'ACTIVE'
                       AND req.enabled = TRUE
                 WHERE ci.code IN (:codes)
                   AND ci.status = 'ACTIVE'
                   AND ci.enabled = TRUE
                   AND req.code NOT IN (
                         SELECT pedido.code
                           FROM catalog_items pedido
                          WHERE pedido.code IN (:codes)
                          UNION
                         SELECT comp.code
                           FROM bundle_components bc
                           JOIN catalog_items b    ON b.id    = bc.bundle_item_id
                           JOIN catalog_items comp ON comp.id = bc.component_item_id
                          WHERE b.code IN (:codes)
                            AND bc.enabled = TRUE
                            AND b.enabled = TRUE
                            AND b.item_type = 'BUNDLE'
                            AND b.status = 'ACTIVE')
                 ORDER BY req.code
                """;

        @Override
        public List<String> findMissingRequirements(Collection<String> codes) {
            if (codes == null || codes.isEmpty()) {
                return List.of();
            }
            Query query = entityManager.createNativeQuery(SQL_MISSING_REQUIREMENTS)
                    .setParameter("codes", codes);
            List<String> faltantes = new ArrayList<>();
            for (Object row : query.getResultList()) {
                faltantes.add(text(row));
            }
            return List.copyOf(faltantes);
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
