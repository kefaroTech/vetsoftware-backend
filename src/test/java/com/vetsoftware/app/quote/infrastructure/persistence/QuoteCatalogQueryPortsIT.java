package com.vetsoftware.app.quote.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CatalogItemRef;
import com.vetsoftware.app.quote.domain.CatalogPriceRef;
import com.vetsoftware.app.quote.domain.ConfiguratorQuestionRef;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.QuoteItemType;
import com.vetsoftware.app.quote.domain.TaxTreatment;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de los cuatro adaptadores de lectura del catálogo, contra MySQL real.
 *
 * <p>
 * <b>Por qué esta rodaja hacía falta.</b> Los cuatro son <b>SQL nativo con
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
 * <b>No se declaran como beans.</b> Los cuatro adaptadores solo necesitan un
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

    /** Lista en borrador: sus precios todavía se pueden cambiar. */
    private static final Long LISTA_BORRADOR = 1970L;

    private static final Long PRECIO_TRAMO_BAJO = 1980L;
    private static final Long PRECIO_TRAMO_ALTO = 1981L;
    private static final Long PRECIO_ANUAL = 1982L;
    private static final Long PRECIO_DESHABILITADO = 1983L;

    private static final Long PREGUNTA = 1990L;
    private static final Long PREGUNTA_DESHABILITADA = 1991L;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaCatalogQueryPorts.JpaCatalogItemQueryPort itemPort;
    private JpaCatalogQueryPorts.JpaPriceListQueryPort priceListPort;
    private JpaCatalogQueryPorts.JpaCatalogPriceQueryPort pricePort;
    private JpaCatalogQueryPorts.JpaConfiguratorQuestionQueryPort questionPort;

    @BeforeEach
    void sembrarElCatalogo() {
        SchemaSeed.seed(entityManager);
        itemPort = new JpaCatalogQueryPorts.JpaCatalogItemQueryPort(entityManager);
        priceListPort = new JpaCatalogQueryPorts.JpaPriceListQueryPort(entityManager);
        pricePort = new JpaCatalogQueryPorts.JpaCatalogPriceQueryPort(entityManager);
        questionPort = new JpaCatalogQueryPorts.JpaConfiguratorQuestionQueryPort(entityManager);

        articulo(ITEM_DEPRECATED, "RETIRADO", "Modulo retirado", "MODULE", null, "DEPRECATED",
                true);
        articulo(ITEM_DRAFT, "BORRADOR", "Modulo en redaccion", "MODULE", null, "DRAFT", true);
        articulo(ITEM_CAPACIDAD, "EXTRA_USER", "Usuario adicional", "CAPACITY", "USER", "ACTIVE",
                true);
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

        pregunta(PREGUNTA, "SELLS_PRODUCTS", true);
        pregunta(PREGUNTA_DESHABILITADA, "PREGUNTA_VIEJA", false);
        entityManager.flush();
    }

    private void articulo(Long id, String code, String name, String itemType, String capacityUnit,
            String status, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_items (id, code, name, item_type, capacity_unit, is_core,
                                           min_quantity, max_quantity, sort_order, status,
                                           created_date, enabled, version)
                VALUES (:id, :code, :name, :itemType, :capacityUnit, false, 1, NULL, 0, :status,
                        '2026-01-01 00:00:00', :enabled, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("code", code).setParameter("name", name)
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

    private void pregunta(Long id, String code, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO configurator_questions (id, code, question_text, help_text,
                                                    answer_type, required, sort_order,
                                                    created_date, enabled, version)
                VALUES (:id, :code, '¿Vende productos?', NULL, 'BOOLEAN', true, 0,
                        '2026-01-01 00:00:00', :enabled, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("code", code)
                .setParameter("enabled", enabled).executeUpdate();
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
            assertThat(itemPort.findActiveById(SchemaSeed.CATALOG_ITEM_CORE_ID))
                    .contains(new CatalogItemRef(SchemaSeed.CATALOG_ITEM_CORE_ID, "CORE",
                            "Nucleo de prueba", QuoteItemType.MODULE));
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
        @DisplayName("cada alias del SELECT cae en su campo: id, código y moneda")
        void cada_alias_cae_en_su_campo() {
            assertThat(priceListPort.findPublishedById(SchemaSeed.PRICE_LIST_ID))
                    .contains(new PriceListRef(SchemaSeed.PRICE_LIST_ID, "LISTA-TEST", "COP"));
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
    @DisplayName("JpaCatalogPriceQueryPort — precio por tramos")
    class Precios {

        @Test
        @DisplayName("cada alias cae en su campo: importe, tasa, tratamiento e incluidas")
        void cada_alias_cae_en_su_campo() {
            assertThat(pricePort.findApplicable(SchemaSeed.PRICE_LIST_ID,
                    SchemaSeed.CATALOG_ITEM_CORE_ID, BillingCycle.MONTHLY, 1))
                    .contains(new CatalogPriceRef(new BigDecimal("100000.00"),
                            new BigDecimal("19.00"), TaxTreatment.TAXED, 2));
        }

        @ParameterizedTest(name = "para {0} unidades el precio unitario es {1}")
        @CsvSource({"1, 12000.00", "9, 12000.00", "10, 12000.00", "11, 9000.00", "15, 9000.00",
                "500, 9000.00"})
        @DisplayName("toma el tramo cuyo tier_min es el más alto de los que no superan la cantidad")
        void toma_el_tramo_mas_alto_que_no_supera_la_cantidad(int cantidad, String esperado) {
            // Sin el ORDER BY tier_min DESC el resultado depende del orden fisico de las
            // filas: para 15 usuarios podria devolver 12.000 en vez de 9.000 y la oferta
            // saldria un 33 % mas cara de lo pactado.
            assertThat(pricePort.findApplicable(SchemaSeed.PRICE_LIST_ID, ITEM_CAPACIDAD,
                    BillingCycle.MONTHLY, cantidad)).get().extracting(CatalogPriceRef::unitAmount)
                    .isEqualTo(new BigDecimal(esperado));
        }

        @Test
        @DisplayName("el tramo abierto por arriba (tier_max nulo) cubre cualquier cantidad")
        void el_tramo_abierto_por_arriba_cubre_cualquier_cantidad() {
            assertThat(pricePort.findApplicable(SchemaSeed.PRICE_LIST_ID, ITEM_CAPACIDAD,
                    BillingCycle.MONTHLY, 100_000)).isPresent();
        }

        @Test
        @DisplayName("el ciclo de facturación acota: un precio anual no vale para una oferta mensual")
        void el_ciclo_de_facturacion_acota() {
            Optional<CatalogPriceRef> anual = pricePort.findApplicable(SchemaSeed.PRICE_LIST_ID,
                    ITEM_CAPACIDAD, BillingCycle.ANNUAL, 1);

            assertThat(anual).get().extracting(CatalogPriceRef::unitAmount)
                    .isEqualTo(new BigDecimal("100000.00"));
            assertThat(anual).get().extracting(CatalogPriceRef::taxTreatment)
                    .isEqualTo(TaxTreatment.EXCLUDED);
        }

        @Test
        @DisplayName("un precio dado de baja no se cotiza")
        void un_precio_dado_de_baja_no_se_cotiza() {
            assertThat(pricePort.findApplicable(SchemaSeed.PRICE_LIST_ID, ITEM_DEPRECATED,
                    BillingCycle.MONTHLY, 1)).isEmpty();
        }

        @Test
        @DisplayName("un artículo sin precio en esa tarifa devuelve vacío")
        void un_articulo_sin_precio_en_esa_tarifa_devuelve_vacio() {
            assertThat(pricePort.findApplicable(LISTA_BORRADOR, ITEM_CAPACIDAD,
                    BillingCycle.MONTHLY, 1)).isEmpty();
        }

        @Test
        @DisplayName("transporta included_quantity, que es lo que R15 necesita para restar")
        void transporta_included_quantity() {
            assertThat(pricePort.findApplicable(SchemaSeed.PRICE_LIST_ID, ITEM_CAPACIDAD,
                    BillingCycle.MONTHLY, 5)).get().extracting(CatalogPriceRef::includedQuantity)
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("JpaConfiguratorQuestionQueryPort")
    class Preguntas {

        @Test
        @DisplayName("lee el id y el código, que es lo único que se copia a la respuesta")
        void lee_el_id_y_el_codigo() {
            assertThat(questionPort.findById(PREGUNTA))
                    .contains(new ConfiguratorQuestionRef(PREGUNTA, "SELLS_PRODUCTS"));
        }

        @Test
        @DisplayName("una pregunta dada de baja ya no se puede congelar en una respuesta")
        void una_pregunta_dada_de_baja_no_se_congela() {
            assertThat(questionPort.findById(PREGUNTA_DESHABILITADA)).isEmpty();
        }

        @Test
        @DisplayName("un id inexistente devuelve vacío")
        void un_id_inexistente_devuelve_vacio() {
            assertThat(questionPort.findById(-1L)).isEmpty();
        }
    }
}
