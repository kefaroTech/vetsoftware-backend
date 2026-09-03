package com.vetsoftware.app.quote.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CatalogItemRef;
import com.vetsoftware.app.quote.domain.CatalogPriceRef;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.QuoteItemType;
import com.vetsoftware.app.quote.domain.TaxTreatment;
import com.vetsoftware.app.quote.domain.TieredPrice;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de los cinco adaptadores de lectura del catálogo, contra MySQL real.
 *
 * <p>
 * <b>Por qué esta rodaja hacía falta.</b> Los cinco son <b>SQL nativo con
 * acceso posicional al resultado</b> ({@code row[0]}, {@code row[1]}…). Un
 * {@code SELECT} con las columnas en otro orden, una columna renombrada en una
 * migración o un {@code valueOf} sobre un enum que ya no tiene ese valor no los
 * detecta el compilador: fallan en ejecución, y hasta hoy ningún test los
 * ejecutaba. Quedan además fuera del alcance de la regla ArchUnit
 * {@code ADAPTADOR_JPA_CON_RODAJA}, que solo mira los
 * {@code Jpa<Algo>Repository}, así que tampoco los cubría la red automática.
 *
 * <p>
 * <b>Los tres filtros que se prueban no son cosméticos.</b> Un artículo en
 * {@code DRAFT} todavía se está redactando y uno {@code DEPRECATED} se retiró
 * de la venta; una lista en {@code DRAFT} tiene precios que aún se pueden
 * cambiar. Congelar cualquiera de los tres en un documento con valor legal es
 * vender algo que la plataforma no ofrece, a un precio que nadie aprobó.
 *
 * <p>
 * <b>No se declaran como beans.</b> Los cinco adaptadores solo necesitan un
 * {@code EntityManager}, así que se construyen a mano: añadirlos al
 * {@code @Import} de la rodaja cambiaría la clave del
 * {@code MergedContextConfiguration} y costaría un arranque de contexto entero,
 * que es justo lo que {@link PersistenceSliceConfig} existe para evitar.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCatalogQueryPorts — el SQL nativo del catálogo contra MySQL real")
class QuoteCatalogQueryPortsIT extends AbstractDataJpaTest {

    /** Artículo retirado de la venta: no se puede cotizar. */
    private static final Long ITEM_DEPRECATED = 1960L;
    /** Artículo todavía en redacción: tampoco. */
    private static final Long ITEM_DRAFT = 1961L;
    /** Artículo de capacidad, con precio por tramos. */
    private static final Long ITEM_CAPACIDAD = 1962L;
    /** Artículo activo pero deshabilitado por baja lógica. */
    private static final Long ITEM_DESHABILITADO = 1963L;

    /**
     * Un plan de verdad: paquete ACTIVE con precio de entrada. Sale en
     * {@code GET /plans}.
     */
    private static final Long ITEM_BUNDLE = 1964L;
    /**
     * Módulo ACTIVE que cuelga de ese plan: también sale, dentro de
     * {@code includes}.
     */
    private static final Long ITEM_COMPONENTE = 1965L;
    /**
     * <b>La pieza clave de esta rodaja.</b> Módulo {@code ACTIVE}, habilitado y con
     * precio de entrada en la tarifa —o sea, indistinguible de un componente por
     * todos los filtros «obvios»— que <b>no cuelga de ningún paquete</b>. Es
     * catálogo interno: {@code GET /plans} no lo anuncia y la autocontratación
     * tampoco puede nombrarlo.
     */
    private static final Long ITEM_INTERNO = 1966L;

    /**
     * La forma exacta de los cuatro {@code EXTRA_*}: {@code CAPACITY}
     * {@code ACTIVE}, tarifada, y <b>sin una sola fila en
     * {@code bundle_components}</b> — se vende aparte y no viene incluida en ningún
     * paquete. Es el artículo que el gate rechazaba y que la columna
     * {@code self_service} existe para admitir.
     */
    private static final Long ITEM_CAPACIDAD_EXTRA = 1967L;

    /** Lista en borrador: sus precios todavía se pueden cambiar. */
    private static final Long LISTA_BORRADOR = 1970L;

    private static final Long PRECIO_TRAMO_BAJO = 1980L;
    private static final Long PRECIO_TRAMO_ALTO = 1981L;
    private static final Long PRECIO_ANUAL = 1982L;
    private static final Long PRECIO_DESHABILITADO = 1983L;
    private static final Long PRECIO_BUNDLE = 1984L;
    private static final Long PRECIO_COMPONENTE = 1985L;
    private static final Long PRECIO_INTERNO = 1986L;
    private static final Long PRECIO_BORRADOR = 1987L;
    private static final Long PRECIO_CAPACIDAD_EXTRA = 1988L;

    private static final Long ENLACE_COMPONENTE = 1995L;
    private static final Long ENLACE_BORRADOR = 1996L;
    /** Cuelga el contador del paquete: sin esto no es catalogo publicado. */
    private static final Long ENLACE_CAPACIDAD = 1997L;

    /** Las dos extrapolaciones que el importe anual NO puede ser. */
    private static final BigDecimal DOCE = new BigDecimal("12");
    private static final BigDecimal DIEZ = new BigDecimal("10");

    private static final String CODE_BUNDLE = "TEST_PLAN_PUBLICADO";
    private static final String CODE_COMPONENTE = "TEST_MODULO_DEL_PLAN";
    private static final String CODE_INTERNO = "TEST_MODULO_INTERNO";
    private static final String CODE_BORRADOR = "BORRADOR";
    private static final String CODE_CAPACIDAD_EXTRA = "TEST_EXTRA_SUELTO";

    @PersistenceContext
    private EntityManager entityManager;

    private JpaCatalogQueryPorts.JpaCatalogItemQueryPort itemPort;
    private JpaCatalogQueryPorts.JpaPriceListQueryPort priceListPort;
    private JpaCatalogQueryPorts.JpaCatalogPriceQueryPort pricePort;
    private JpaCatalogQueryPorts.JpaPublishedCatalogItemQueryPort publicadoPort;

    /** Resuelto, no sembrado: el articulo CORE llega del changeset 308. */
    private Long nucleo;

    @BeforeEach
    void sembrarElCatalogo() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
        itemPort = new JpaCatalogQueryPorts.JpaCatalogItemQueryPort(entityManager);
        priceListPort = new JpaCatalogQueryPorts.JpaPriceListQueryPort(entityManager);
        pricePort = new JpaCatalogQueryPorts.JpaCatalogPriceQueryPort(entityManager);
        publicadoPort = new JpaCatalogQueryPorts.JpaPublishedCatalogItemQueryPort(entityManager);

        articulo(ITEM_DEPRECATED, "RETIRADO", "Modulo retirado", "MODULE", null, "DEPRECATED",
                true);
        articulo(ITEM_DRAFT, CODE_BORRADOR, "Modulo en redaccion", "MODULE", null, "DRAFT", true);
        // TEST_EXTRA_USER y no EXTRA_USER: ese codigo lo ocupa el changeset 308 desde
        // que el catalogo comercial se siembra en todos los entornos. Al chocar contra
        // uq_catalog_items_code el ON DUPLICATE KEY de articulo() lo convertia en un
        // no-op, ITEM_CAPACIDAD no llegaba a existir y los precios de abajo morian con
        // una violacion de fk_catalog_prices_item que no mencionaba el catalogo por
        // ninguna parte. Es #647 otra vez, en otro fichero.
        articulo(ITEM_CAPACIDAD, "TEST_EXTRA_USER", "Usuario adicional", "CAPACITY", "USER",
                "ACTIVE", true);
        articulo(ITEM_DESHABILITADO, "DE_BAJA", "Modulo de baja", "MODULE", null, "ACTIVE", false);

        listaEnBorrador();

        // Tramos del articulo de capacidad: 1-10 a 12.000, y del 11 en adelante a
        // 9.000. Es el ejemplo exacto del javadoc del adaptador.
        precio(PRECIO_TRAMO_BAJO, SchemaSeed.PRICE_LIST_ID, ITEM_CAPACIDAD, "MONTHLY", 1, 10, 2,
                "12000.00", "19.00", "TAXED", true);
        precio(PRECIO_TRAMO_ALTO, SchemaSeed.PRICE_LIST_ID, ITEM_CAPACIDAD, "MONTHLY", 11, null, 2,
                "9000.00", "19.00", "TAXED", true);
        // Mismo articulo, otro ciclo: no debe aparecer en las consultas MONTHLY.
        precio(PRECIO_ANUAL, SchemaSeed.PRICE_LIST_ID, ITEM_CAPACIDAD, "ANNUAL", 1, null, 0,
                "100000.00", "0.00", "EXCLUDED", true);
        precio(PRECIO_DESHABILITADO, SchemaSeed.PRICE_LIST_ID, ITEM_DEPRECATED, "MONTHLY", 1, null,
                0, "5000.00", "0.00", "EXEMPT", false);

        sembrarElCatalogoPublicable();
        entityManager.flush();
    }

    /**
     * El escenario de {@link ArticulosPublicados}, montado para que los tres
     * artículos <b>solo se diferencien en lo que la regla mira</b>.
     *
     * <p>
     * Los tres son {@code MODULE}/{@code BUNDLE}, están habilitados y tienen precio
     * de entrada ({@code tier_min = 1}) en la misma tarifa y el mismo ciclo. Lo
     * único que cambia es: el plan y su componente cuelgan de un paquete
     * {@code ACTIVE}; {@link #ITEM_INTERNO} no cuelga de ninguno; y
     * {@link #ITEM_DRAFT} cuelga de uno pero está en {@code DRAFT}. Si el escenario
     * no fuera así de simétrico, un test en verde no probaría cuál de los cinco
     * predicados hizo el trabajo.
     */
    private void sembrarElCatalogoPublicable() {
        articulo(ITEM_BUNDLE, CODE_BUNDLE, "Plan publicado", "BUNDLE", null, "ACTIVE", true);
        articulo(ITEM_COMPONENTE, CODE_COMPONENTE, "Modulo del plan", "MODULE", null, "ACTIVE",
                true);
        articulo(ITEM_INTERNO, CODE_INTERNO, "Modulo interno", "MODULE", null, "ACTIVE", true);
        articulo(ITEM_CAPACIDAD_EXTRA, CODE_CAPACIDAD_EXTRA, "Sede adicional", "CAPACITY", "BRANCH",
                "ACTIVE", true);

        componenteDePaquete(ENLACE_COMPONENTE, ITEM_BUNDLE, ITEM_COMPONENTE);
        // El de borrador SI cuelga del paquete: asi el unico motivo por el que no se
        // puede contratar es su status, y el test lo demuestra en vez de suponerlo.
        componenteDePaquete(ENLACE_BORRADOR, ITEM_BUNDLE, ITEM_DRAFT);
        // Y el contador tambien cuelga del paquete: es lo que GET /plans anuncia
        // dentro de `capacities`, y lo que la autocontratacion tiene que poder
        // nombrar. Ya viene tarifado en los DOS ciclos (MONTHLY por tramos, ANNUAL
        // en una sola fila), asi que es el articulo con el que se prueba que la
        // capacidad extra anual se resuelve y se cotiza.
        componenteDePaquete(ENLACE_CAPACIDAD, ITEM_BUNDLE, ITEM_CAPACIDAD);

        precio(PRECIO_BUNDLE, SchemaSeed.PRICE_LIST_ID, ITEM_BUNDLE, "MONTHLY", 1, null, 0,
                "250000.00", "19.00", "TAXED", true);
        precio(PRECIO_COMPONENTE, SchemaSeed.PRICE_LIST_ID, ITEM_COMPONENTE, "MONTHLY", 1, null, 0,
                "30000.00", "19.00", "TAXED", true);
        precio(PRECIO_INTERNO, SchemaSeed.PRICE_LIST_ID, ITEM_INTERNO, "MONTHLY", 1, null, 0,
                "40000.00", "19.00", "TAXED", true);
        precio(PRECIO_BORRADOR, SchemaSeed.PRICE_LIST_ID, ITEM_DRAFT, "MONTHLY", 1, null, 0,
                "10000.00", "19.00", "TAXED", true);
        precio(PRECIO_CAPACIDAD_EXTRA, SchemaSeed.PRICE_LIST_ID, ITEM_CAPACIDAD_EXTRA, "MONTHLY", 1,
                null, 0, "45000.00", "19.00", "TAXED", true);
    }

    /**
     * {@code chk_catalog_items_self_service} solo la admite en {@code MODULE} y
     * {@code CAPACITY}: marcar un {@code BUNDLE} o un {@code ONE_TIME} muere en la
     * base y no en la aserción.
     */
    private void marcarAutoservicio(Long catalogItemId) {
        entityManager
                .createNativeQuery("UPDATE catalog_items SET self_service = TRUE WHERE id = :id")
                .setParameter("id", catalogItemId).executeUpdate();
        entityManager.flush();
    }

    private void componenteDePaquete(Long id, Long bundleId, Long componentId) {
        entityManager.createNativeQuery("""
                INSERT INTO bundle_components (id, bundle_item_id, component_item_id, quantity,
                                               created_date, enabled)
                VALUES (:id, :paquete, :componente, 1, '2026-01-01 00:00:00', true)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("paquete", bundleId)
                .setParameter("componente", componentId).executeUpdate();
    }

    private void articulo(Long id, String code, String name, String itemType, String capacityUnit,
            String status, boolean enabled) {
        entityManager
                .createNativeQuery(
                        """
                                INSERT INTO catalog_items (id, code, name, item_type, capacity_unit, structural_minimum,
                                                           min_quantity, max_quantity, sort_order, status,
                                                           trial_eligibility, default_trial_days, trial_outcome,
                                                           service_nature, created_date, enabled, version)
                                VALUES (:id, :code, :name, :itemType, :capacityUnit, false, 1, NULL, 0, :status,
                                        'NEVER_FREE', NULL, NULL, 'SOFTWARE_LICENSING',
                                        '2026-01-01 00:00:00', :enabled, 0)
                                ON DUPLICATE KEY UPDATE id = id
                                """)
                .setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .setParameter("itemType", itemType).setParameter("capacityUnit", capacityUnit)
                .setParameter("status", status).setParameter("enabled", enabled).executeUpdate();
    }

    private void listaEnBorrador() {
        entityManager.createNativeQuery("""
                INSERT INTO price_lists (id, code, name, currency, valid_from, status,
                                         published_at, published_by_system_user_id,
                                         created_date, enabled, version)
                VALUES (:id, 'LISTA-BORRADOR', 'Lista en borrador', 'COP', '2026-01-01', 'DRAFT',
                        NULL, NULL, '2026-01-01 00:00:00', true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", LISTA_BORRADOR).executeUpdate();
    }

    private void precio(Long id, Long priceListId, Long catalogItemId, String cycle, int tierMin,
            Integer tierMax, int included, String unitAmount, String taxRate, String taxTreatment,
            boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_prices (id, price_list_id, catalog_item_id, billing_cycle,
                                            tier_min, tier_max, included_quantity, unit_amount,
                                            setup_amount, tax_rate, tax_treatment,
                                            created_date, enabled, version)
                VALUES (:id, :lista, :articulo, :ciclo, :tierMin, :tierMax, :incluidas, :importe,
                        0.00, :tasa, :tratamiento, '2026-01-01 00:00:00', :enabled, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("lista", priceListId)
                .setParameter("articulo", catalogItemId).setParameter("ciclo", cycle)
                .setParameter("tierMin", tierMin).setParameter("tierMax", tierMax)
                .setParameter("incluidas", included)
                .setParameter("importe", new BigDecimal(unitAmount))
                .setParameter("tasa", new BigDecimal(taxRate))
                .setParameter("tratamiento", taxTreatment).setParameter("enabled", enabled)
                .executeUpdate();
    }

    @Nested
    @DisplayName("JpaCatalogItemQueryPort")
    class Articulos {

        @Test
        @DisplayName("cada alias del SELECT cae en el campo que dice: id, código, nombre y tipo")
        void cada_alias_cae_en_el_campo_que_dice() {
            // La comprobacion de fondo de esta rodaja: el mapeo es POSICIONAL. Si alguien
            // reordena el SELECT, el codigo acaba en el nombre y nadie se entera hasta
            // que un cliente ve "CORE" como nombre de producto en su cotizacion.
            assertThat(itemPort.findActiveById(nucleo)).contains(new CatalogItemRef(nucleo, "CORE",
                    "Núcleo: clientes y mascotas", QuoteItemType.MODULE));
        }

        @Test
        @DisplayName("resuelve el tipo CAPACITY, que es el que dispara la resta de R15")
        void resuelve_el_tipo_capacity() {
            assertThat(itemPort.findActiveById(ITEM_CAPACIDAD)).get()
                    .extracting(CatalogItemRef::itemType).isEqualTo(QuoteItemType.CAPACITY);
        }

        @Test
        @DisplayName("solo un artículo ACTIVE se puede cotizar: ni DEPRECATED ni DRAFT")
        void solo_un_articulo_active_se_puede_cotizar() {
            // Se comprueba primero que las dos filas EXISTEN. Sin eso, un id que derivara
            // haria pasar el test por el motivo equivocado -vacio porque no hay fila, no
            // porque el filtro de estado funcione-, que es la forma silenciosa de que un
            // test deje de proteger.
            assertThat(filasConId(ITEM_DEPRECATED)).isEqualTo(1);
            assertThat(filasConId(ITEM_DRAFT)).isEqualTo(1);

            assertThat(itemPort.findActiveById(ITEM_DEPRECATED)).isEmpty();
            assertThat(itemPort.findActiveById(ITEM_DRAFT)).isEmpty();
        }

        private long filasConId(Long id) {
            return ((Number) entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM catalog_items WHERE id = :id")
                    .setParameter("id", id).getSingleResult()).longValue();
        }

        @Test
        @DisplayName("un artículo dado de baja tampoco se cotiza, aunque su estado sea ACTIVE")
        void un_articulo_dado_de_baja_tampoco_se_cotiza() {
            // status y enabled son dos cosas distintas: el primero es el ciclo comercial
            // y el segundo la baja logica. El SQL filtra por los dos.
            assertThat(itemPort.findActiveById(ITEM_DESHABILITADO)).isEmpty();
        }

        @Test
        @DisplayName("un id inexistente devuelve vacío, no null ni excepción")
        void un_id_inexistente_devuelve_vacio() {
            assertThat(itemPort.findActiveById(-1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("JpaPriceListQueryPort")
    class Tarifas {

        @Test
        @DisplayName("cada alias del SELECT cae en su campo: id, código, moneda y la ventana de"
                + " vigencia (COT-020)")
        void cada_alias_cae_en_su_campo() {
            // La ventana es la que siembra SchemaSeed: valid_from el 2026-01-01 y
            // valid_to SIN valor. Ese null no es un hueco del fixture, es el caso real
            // -la lista viva del catálogo se publica sin fecha de fin- y aquí se
            // comprueba que el mapeo lo conserva en vez de convertirlo en una fecha.
            assertThat(priceListPort.findPublishedById(SchemaSeed.PRICE_LIST_ID))
                    .contains(new PriceListRef(SchemaSeed.PRICE_LIST_ID, "LISTA-TEST", "COP",
                            LocalDate.of(2026, 1, 1), null));
        }

        /**
         * <b>El otro extremo de la ventana, que hasta hoy no ejercitaba nadie.</b> El
         * andamio publica la lista SIN fecha de fin, asi que {@code date(row[4])} solo
         * se habia ejecutado sobre un nulo: la rama que convierte una columna
         * {@code DATE} con valor viajaba sin red desde que se escribio. Y no la ve el
         * compilador, porque una consulta nativa entrega {@code Object} y el reparto de
         * tipos ocurre en ejecucion.
         */
        @Test
        @DisplayName("una lista con cierre trae las DOS fechas de la ventana, no solo el inicio")
        void una_lista_con_cierre_trae_las_dos_fechas() {
            entityManager
                    .createNativeQuery(
                            "UPDATE price_lists SET valid_to = '2026-12-31' WHERE id = :id")
                    .setParameter("id", SchemaSeed.PRICE_LIST_ID).executeUpdate();
            entityManager.flush();
            entityManager.clear();

            assertThat(priceListPort.findPublishedById(SchemaSeed.PRICE_LIST_ID))
                    .contains(new PriceListRef(SchemaSeed.PRICE_LIST_ID, "LISTA-TEST", "COP",
                            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
        }

        /**
         * <b>Que entrega el driver para una columna {@code DATE}.</b> El helper
         * {@code JpaCatalogQueryPorts.date(Object)} lleva cuatro caminos —
         * {@code LocalDate}, {@code java.sql.Date}, {@code Timestamp} y un
         * {@code parse} del texto— escritos «por si acaso»: compilan todos, pero contra
         * MySQL de verdad solo se recorre UNO, y hasta ahora nadie sabia cual. Este
         * caso lo nombra, de modo que si un cambio de driver mueve el tipo el fallo
         * salga aqui —con el nombre de la clase que llego— y no disfrazado de tarifa
         * que de pronto no se resuelve.
         */
        @Test
        @DisplayName("el driver entrega la columna DATE como LocalDate: es la rama viva del mapeo")
        void el_driver_entrega_la_columna_date_como_local_date() {
            Object crudo = entityManager
                    .createNativeQuery("SELECT valid_from FROM price_lists WHERE id = :id")
                    .setParameter("id", SchemaSeed.PRICE_LIST_ID).getSingleResult();

            assertThat(crudo).isInstanceOf(LocalDate.class).isEqualTo(LocalDate.of(2026, 1, 1));
        }

        @Test
        @DisplayName("una lista en borrador no sirve para cotizar: sus precios aún se mueven")
        void una_lista_en_borrador_no_sirve_para_cotizar() {
            assertThat(priceListPort.findPublishedById(LISTA_BORRADOR)).isEmpty();
        }

        @Test
        @DisplayName("una lista dada de baja tampoco, aunque estuviera publicada")
        void una_lista_dada_de_baja_tampoco() {
            entityManager.createNativeQuery("UPDATE price_lists SET enabled = false WHERE id = :id")
                    .setParameter("id", SchemaSeed.PRICE_LIST_ID).executeUpdate();
            entityManager.flush();

            assertThat(priceListPort.findPublishedById(SchemaSeed.PRICE_LIST_ID)).isEmpty();
        }

        @Test
        @DisplayName("un id inexistente devuelve vacío, no null ni excepción")
        void un_id_inexistente_devuelve_vacio() {
            assertThat(priceListPort.findPublishedById(-1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("JpaCatalogPriceQueryPort — los tramos, TODOS (D-66)")
    class Precios {

        @Test
        @DisplayName("cada alias cae en su campo: importe, tasa, tratamiento, incluidas y tramo")
        void cada_alias_cae_en_su_campo() {
            assertThat(
                    pricePort.findAllTiers(SchemaSeed.PRICE_LIST_ID, nucleo, BillingCycle.MONTHLY))
                    .containsExactly(new CatalogPriceRef(new BigDecimal("100000.00"),
                            new BigDecimal("19.00"), TaxTreatment.TAXED, 2, 1, null));
        }

        @Test
        @DisplayName("quince usuarios se cobran por tramos ACUMULATIVOS: diez a 12.000 mas tres a "
                + "9.000 dan 147.000, no trece a 9.000")
        void quince_usuarios_se_cobran_por_tramos_acumulativos() {
            // ESTE TEST SUSTITUYE al que afirmaba «para 15 unidades el precio unitario es
            // 9.000», que no era un falso verde sino la prueba que DEFENDIA el defecto:
            // daba por correcta la aritmetica plana que D-66 declara incorrecta. La
            // consulta ya no elige tramo —devuelve los dos— y el reparto lo hace el
            // dominio, que es donde se puede comprobar.
            List<CatalogPriceRef> tramos = pricePort.findAllTiers(SchemaSeed.PRICE_LIST_ID,
                    ITEM_CAPACIDAD, BillingCycle.MONTHLY);

            TieredPrice repartido = TieredPrice.of(QuoteItemType.CAPACITY, 15, tramos);

            assertThat(repartido.includedQuantity()).isEqualTo(2);
            assertThat(repartido.tiers()).hasSize(2);

            BigDecimal total = BigDecimal.ZERO;
            for (CatalogPriceRef tramo : repartido.tiers()) {
                int unidades = tramo.unitsWithin(15 - repartido.includedQuantity());
                total = total.add(tramo.unitAmount().multiply(BigDecimal.valueOf(unidades)));
            }

            assertThat(total).isEqualByComparingTo("147000.00");
            // Lo que devolvia la consulta vieja: trece unidades al precio del tramo alto.
            assertThat(total).isNotEqualByComparingTo("117000.00");
        }

        @Test
        @DisplayName("devuelve los tramos ordenados por tier_min, sin recortar por cantidad")
        void devuelve_los_tramos_ordenados_sin_recortar() {
            List<CatalogPriceRef> tramos = pricePort.findAllTiers(SchemaSeed.PRICE_LIST_ID,
                    ITEM_CAPACIDAD, BillingCycle.MONTHLY);

            assertThat(tramos).extracting(CatalogPriceRef::tierMin).containsExactly(1, 11);
            assertThat(tramos).extracting(CatalogPriceRef::tierMax).containsExactly(10, null);
            assertThat(tramos).extracting(CatalogPriceRef::unitAmount)
                    .containsExactly(new BigDecimal("12000.00"), new BigDecimal("9000.00"));
        }

        @Test
        @DisplayName("una cantidad que cabe entera en el primer tramo no arrastra el segundo")
        void una_cantidad_que_cabe_en_el_primer_tramo() {
            TieredPrice repartido = TieredPrice.of(QuoteItemType.CAPACITY, 5, pricePort
                    .findAllTiers(SchemaSeed.PRICE_LIST_ID, ITEM_CAPACIDAD, BillingCycle.MONTHLY));

            assertThat(repartido.tiers()).hasSize(1);
            assertThat(repartido.tiers().get(0).unitAmount())
                    .isEqualByComparingTo(new BigDecimal("12000.00"));
        }

        @Test
        @DisplayName("el ciclo de facturación acota: un precio anual no vale para una oferta mensual")
        void el_ciclo_de_facturacion_acota() {
            List<CatalogPriceRef> anual = pricePort.findAllTiers(SchemaSeed.PRICE_LIST_ID,
                    ITEM_CAPACIDAD, BillingCycle.ANNUAL);

            assertThat(anual).singleElement().satisfies(tramo -> {
                assertThat(tramo.unitAmount()).isEqualTo(new BigDecimal("100000.00"));
                assertThat(tramo.taxTreatment()).isEqualTo(TaxTreatment.EXCLUDED);
            });
        }

        @Test
        @DisplayName("un precio dado de baja no se cotiza")
        void un_precio_dado_de_baja_no_se_cotiza() {
            assertThat(pricePort.findAllTiers(SchemaSeed.PRICE_LIST_ID, ITEM_DEPRECATED,
                    BillingCycle.MONTHLY)).isEmpty();
        }

        @Test
        @DisplayName("un artículo sin precio en esa tarifa devuelve vacío")
        void un_articulo_sin_precio_en_esa_tarifa_devuelve_vacio() {
            assertThat(pricePort.findAllTiers(LISTA_BORRADOR, ITEM_CAPACIDAD, BillingCycle.MONTHLY))
                    .isEmpty();
        }

        @Test
        @DisplayName("transporta included_quantity, que es lo que R15 necesita para restar")
        void transporta_included_quantity() {
            assertThat(pricePort.findAllTiers(SchemaSeed.PRICE_LIST_ID, ITEM_CAPACIDAD,
                    BillingCycle.MONTHLY)).first().extracting(CatalogPriceRef::includedQuantity)
                    .isEqualTo(2);
        }
    }

    /**
     * <b>El traductor {@code code -> id} de la autocontratación, que es también el
     * gate.</b> Sin él, {@code POST /quotes/self-serve} era inalcanzable —pedía un
     * {@code catalogItemId} que ninguna respuesta alcanzable por el tenant
     * publica—; con un traductor descuidado sería la puerta de atrás de
     * {@code GET /catalog-items}, que está cerrado a {@code SYSTEM}.
     *
     * <p>
     * <b>Esta rodaja es el único sitio donde eso se puede probar.</b> El test
     * unitario del caso de uso mockea el puerto, así que allí «no publicable» es
     * una premisa; aquí es el {@code WHERE} ejecutándose contra MySQL con las filas
     * delante. Y el adaptador queda fuera del alcance de
     * {@code ADAPTADOR_JPA_CON_RODAJA} —solo mira los {@code Jpa<Algo>Repository}—,
     * así que nada lo obliga a existir salvo esta decisión.
     *
     * <p>
     * <b>Lo que estos tests NO comprueban</b>, dicho para que nadie lo dé por
     * hecho: que este conjunto coincida <i>exactamente</i> con el que devuelve
     * {@code GET /plans}. Son dos consultas distintas —esta y
     * {@code JpaPublicPlanQueryPort}— escritas para casar, y hoy casan por
     * revisión, no por construcción. Si {@code SQL_PLANS} o {@code SQL_COMPONENTS}
     * cambian de criterio y esta no, los tests de aquí seguirán en verde y el
     * traductor se habrá desalineado del catálogo público en silencio.
     */
    @Nested
    @DisplayName("JpaPublishedCatalogItemQueryPort — el rótulo solo se resuelve si está publicado")
    class ArticulosPublicados {

        @Test
        @DisplayName("un paquete ACTIVE con precio de entrada se resuelve a su id")
        void un_paquete_publicado_se_resuelve() {
            assertThat(publicadoPort.findPublishedIdByCode(CODE_BUNDLE, SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY)).contains(ITEM_BUNDLE);
        }

        @Test
        @DisplayName("un módulo que cuelga de ese paquete también: es lo que la portada anuncia")
        void un_componente_del_paquete_se_resuelve() {
            assertThat(publicadoPort.findPublishedIdByCode(CODE_COMPONENTE,
                    SchemaSeed.PRICE_LIST_ID, BillingCycle.MONTHLY)).contains(ITEM_COMPONENTE);
        }

        /**
         * <b>El caso que justifica el puerto entero.</b> {@link #ITEM_INTERNO} es
         * {@code ACTIVE}, está habilitado y tiene precio en la tarifa: pasa todos los
         * filtros que un traductor ingenuo comprobaría. Lo único que no tiene es un
         * paquete del que colgar, y por eso {@code GET /plans} no lo anuncia. Si esta
         * consulta lo devolviera, bastaría probar rótulos contra
         * {@code POST /quotes/self-serve} para enumerar el catálogo interno de la
         * plataforma —justo lo que {@code GET /catalog-items} evita cerrándose a
         * {@code SYSTEM}—.
         */
        @Test
        @DisplayName("un módulo activo y tarifado que no cuelga de ningún paquete NO se resuelve")
        void un_modulo_interno_no_se_resuelve() {
            assertThat(publicadoPort.findPublishedIdByCode(CODE_INTERNO, SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY)).isEmpty();
        }

        /**
         * Y aquí el rótulo <b>existe</b> en {@code catalog_items}, cuelga del paquete
         * publicado y tiene precio: lo único que lo frena es su {@code status}. El
         * resultado tiene que ser el mismo vacío que da un código que no existe, sin
         * ningún dato que permita separarlos — ver el test de abajo.
         */
        @Test
        @DisplayName("un módulo en DRAFT no se resuelve aunque cuelgue del paquete y esté tarifado")
        void un_modulo_en_borrador_no_se_resuelve() {
            assertThat(publicadoPort.findPublishedIdByCode(CODE_BORRADOR, SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY)).isEmpty();
        }

        /**
         * <b>La otra mitad de la prueba del oráculo.</b> El caso de uso ya garantiza
         * que el mensaje es el mismo ({@code SelfServeQuoteServiceTest}); esto
         * garantiza que <b>el dato también lo es</b>. Dos {@link java.util.Optional}
         * vacíos son indistinguibles por construcción: no hay excepción distinta, ni
         * código de motivo, ni {@code null} contra vacío que el caso de uso pudiera
         * ramificar aunque quisiera.
         */
        @Test
        @DisplayName("el código interno y el inexistente devuelven el mismo vacío, sin motivo")
        void el_interno_y_el_inexistente_son_el_mismo_vacio() {
            assertThat(publicadoPort.findPublishedIdByCode(CODE_INTERNO, SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY))
                    .isEqualTo(publicadoPort.findPublishedIdByCode("NO_EXISTE_ESTE_ROTULO",
                            SchemaSeed.PRICE_LIST_ID, BillingCycle.MONTHLY));
        }

        @Test
        @DisplayName("un artículo retirado de la venta no se resuelve")
        void un_articulo_retirado_no_se_resuelve() {
            assertThat(publicadoPort.findPublishedIdByCode("RETIRADO", SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY)).isEmpty();
        }

        /**
         * El paquete está tarifado solo en mensual. Pedirlo en anual no es «el precio
         * anual sale del mensual»: es que ese plan no se publicó para ese ciclo, y una
         * cotización anual con precio mensual sería un importe que nadie aprobó.
         */
        @Test
        @DisplayName("un paquete tarifado solo en mensual no se resuelve para el ciclo anual")
        void un_paquete_sin_precio_en_el_ciclo_no_se_resuelve() {
            assertThat(publicadoPort.findPublishedIdByCode(CODE_BUNDLE, SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.ANNUAL)).isEmpty();
        }

        /**
         * <b>El camino que nunca se habia ejercitado: capacidad extra en ciclo
         * anual.</b> {@link #ITEM_CAPACIDAD} es un {@code CAPACITY} que cuelga del
         * paquete publicado y tiene precio de entrada en {@code ANNUAL}, asi que el
         * {@code JOIN} por ciclo lo deja pasar y la autocontratacion puede nombrarlo.
         *
         * <p>
         * Antes esto era inalcanzable en la practica: {@code GET /plans} publicaba de
         * cada contador un solo importe —el mensual, y sin decirlo en el nombre—, asi
         * que quien pintaba un plan anual no tenia forma de saber si el contador
         * siquiera existia en ese ciclo. Si no existia, el rechazo llegaba en la
         * contratacion y era —a proposito— indistinguible de «codigo desconocido».
         */
        @Test
        @DisplayName("una capacidad del paquete tarifada en anual SI se resuelve para el ciclo"
                + " anual")
        void una_capacidad_tarifada_en_anual_se_resuelve() {
            assertThat(publicadoPort.findPublishedIdByCode("TEST_EXTRA_USER",
                    SchemaSeed.PRICE_LIST_ID, BillingCycle.ANNUAL)).contains(ITEM_CAPACIDAD);
        }

        /**
         * Resolver no basta: hay que comprobar que lo que se cobra sale de la fila del
         * ciclo pedido. El contador vale 12.000 al mes en su tramo de entrada y 100.000
         * al ano; 100.000 no es 12.000 por doce (144.000) ni por diez (120.000), asi
         * que un precio extrapolado en vez de leido falla aqui.
         *
         * <p>
         * Es exactamente la diferencia que el cliente veia: la portada solo sabia el
         * mensual y el front lo multiplicaba, mientras esta escalera —la que usa
         * {@code CreateQuoteService}— dice otra cosa.
         */
        @Test
        @DisplayName("y se cotiza con la escalera ANUAL del articulo, no con el mensual"
                + " extrapolado")
        void una_capacidad_anual_se_cotiza_con_su_propia_escalera() {
            Long resuelto = publicadoPort.findPublishedIdByCode("TEST_EXTRA_USER",
                    SchemaSeed.PRICE_LIST_ID, BillingCycle.ANNUAL).orElseThrow();

            List<CatalogPriceRef> anual = pricePort.findAllTiers(SchemaSeed.PRICE_LIST_ID, resuelto,
                    BillingCycle.ANNUAL);
            List<CatalogPriceRef> mensual = pricePort.findAllTiers(SchemaSeed.PRICE_LIST_ID,
                    resuelto, BillingCycle.MONTHLY);

            assertThat(anual).singleElement().extracting(CatalogPriceRef::unitAmount)
                    .isEqualTo(new BigDecimal("100000.00"));
            assertThat(mensual).first().extracting(CatalogPriceRef::unitAmount)
                    .isEqualTo(new BigDecimal("12000.00"));
            assertThat(anual.get(0).unitAmount())
                    .isNotEqualByComparingTo(mensual.get(0).unitAmount().multiply(DOCE))
                    .isNotEqualByComparingTo(mensual.get(0).unitAmount().multiply(DIEZ));
        }

        @Test
        @DisplayName("el mismo rótulo en otra tarifa, donde no está tarifado, tampoco se resuelve")
        void el_mismo_rotulo_en_otra_tarifa_no_se_resuelve() {
            assertThat(publicadoPort.findPublishedIdByCode(CODE_BUNDLE, LISTA_BORRADOR,
                    BillingCycle.MONTHLY)).isEmpty();
        }

        /**
         * <b>La capacidad extra, que es el caso que abrió la columna.</b>
         * {@link #ITEM_CAPACIDAD_EXTRA} tiene la forma de los cuatro {@code EXTRA_*}:
         * {@code CAPACITY} {@code ACTIVE}, tarifada y sin colgar de ningún paquete. Con
         * el gate atado solo a {@code bundle_components}, cotizar una sede adicional
         * daba 400 y tumbaba la petición entera — y colgarla de un pack para arreglarlo
         * la habría anunciado como incluida en su precio.
         */
        @Test
        @DisplayName("una capacidad suelta marcada self_service SÍ se resuelve")
        void una_capacidad_marcada_self_service_se_resuelve() {
            assertThat(publicadoPort.findPublishedIdByCode(CODE_CAPACIDAD_EXTRA,
                    SchemaSeed.PRICE_LIST_ID, BillingCycle.MONTHLY)).isEmpty();

            marcarAutoservicio(ITEM_CAPACIDAD_EXTRA);

            assertThat(publicadoPort.findPublishedIdByCode(CODE_CAPACIDAD_EXTRA,
                    SchemaSeed.PRICE_LIST_ID, BillingCycle.MONTHLY)).contains(ITEM_CAPACIDAD_EXTRA);
        }

        /**
         * La unión es <b>aditiva</b>: la vía nueva se suma a la de
         * {@code bundle_components}, no la reemplaza. Sin este caso, escribir el gate
         * como «solo {@code self_service}» dejaría los trece módulos de los tres packs
         * fuera de la contratación y ningún otro test lo delataría, porque todos los de
         * arriba miran el rótulo de uno en uno.
         */
        @Test
        @DisplayName("marcar una capacidad no saca del gate a lo que ya colgaba del paquete")
        void marcar_una_capacidad_no_saca_a_lo_que_colgaba_del_paquete() {
            marcarAutoservicio(ITEM_CAPACIDAD_EXTRA);

            assertThat(publicadoPort.findPublishedIdByCode(CODE_BUNDLE, SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY)).contains(ITEM_BUNDLE);
            assertThat(publicadoPort.findPublishedIdByCode(CODE_COMPONENTE,
                    SchemaSeed.PRICE_LIST_ID, BillingCycle.MONTHLY)).contains(ITEM_COMPONENTE);
            assertThat(publicadoPort.findPublishedIdByCode(CODE_INTERNO, SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY)).isEmpty();
        }

        /**
         * La marca abre una vía <b>dentro</b> del {@code WHERE}, no por encima de él:
         * el estado, la baja lógica y el precio de entrada en el ciclo pedido siguen
         * mandando. Un artículo en borrador marcado como vendible sigue sin poder
         * cotizarse, que es lo que separa «abrir el gate» de «quitarlo».
         */
        @Test
        @DisplayName("la marca no salta el estado ni el precio del ciclo pedido")
        void la_marca_no_salta_el_estado_ni_el_precio_del_ciclo() {
            marcarAutoservicio(ITEM_DRAFT);
            marcarAutoservicio(ITEM_DESHABILITADO);
            marcarAutoservicio(ITEM_CAPACIDAD_EXTRA);

            assertThat(publicadoPort.findPublishedIdByCode(CODE_BORRADOR, SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY)).isEmpty();
            assertThat(publicadoPort.findPublishedIdByCode("DE_BAJA", SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY)).isEmpty();
            assertThat(publicadoPort.findPublishedIdByCode(CODE_CAPACIDAD_EXTRA,
                    SchemaSeed.PRICE_LIST_ID, BillingCycle.ANNUAL)).isEmpty();
        }

        @Test
        @DisplayName("un rótulo que no existe devuelve vacío")
        void un_rotulo_inexistente_devuelve_vacio() {
            assertThat(publicadoPort.findPublishedIdByCode("NO_EXISTE_ESTE_ROTULO",
                    SchemaSeed.PRICE_LIST_ID, BillingCycle.MONTHLY)).isEmpty();
        }

        /**
         * Un {@code code} vacío no llega a la base. Es un cliente el que lo elige, y
         * puede repetirlo a voluntad: la guarda evita gastar una consulta por cada
         * cadena en blanco que alguien mande.
         */
        @Test
        @DisplayName("un código nulo o en blanco devuelve vacío sin consultar")
        void un_codigo_en_blanco_devuelve_vacio() {
            assertThat(publicadoPort.findPublishedIdByCode(null, SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY)).isEmpty();
            assertThat(publicadoPort.findPublishedIdByCode("   ", SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY)).isEmpty();
            assertThat(publicadoPort.findPublishedIdByCode(CODE_BUNDLE, null, BillingCycle.MONTHLY))
                    .isEmpty();
        }
    }
}
