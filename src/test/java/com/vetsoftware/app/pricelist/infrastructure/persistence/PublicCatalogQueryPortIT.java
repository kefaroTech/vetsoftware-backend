package com.vetsoftware.app.pricelist.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.pricelist.application.dto.PublicCatalogItemRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackComponentRowDto;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * La rodaja que le faltaba a {@link JpaPublicCatalogQueryPort}, y que su propio
 * javadoc reclama: sus tres consultas nativas quedan <b>fuera</b> de
 * {@code ADAPTADOR_JPA_CON_RODAJA} —esa regla solo alcanza a los
 * {@code Jpa<Algo>Repository}—, asi que sin esta clase no las ejecutaria nadie
 * en el build. Es exactamente como sobrevivio meses la incidencia #196.
 *
 * <p>
 * <b>Aqui el SQL decide cuanto se le cobra a alguien</b>, asi que lo que se
 * fija son las cuatro cosas que un mock no puede comprobar:
 *
 * <ul>
 * <li><b>{@code is_core} llega como {@code Byte}, no como {@code Boolean}.</b>
 * MySQL entrega {@code TINYINT} asi y nadie lo convierte solo: es la clase de
 * defecto de #472, que tumbo el alta de empresa entera. Un mock devuelve el
 * booleano que le pidas y no ve nada.</li>
 * <li><b>El {@code EXISTS} que decide {@code selfServiceEligible}</b> tiene que
 * dar el mismo veredicto que el gate de la autocontratacion. Se siembra un
 * modulo que cuelga de un paquete publicado y otro que no cuelga de
 * ninguno.</li>
 * <li><b>Las dos columnas {@code included_quantity}</b>, una por ciclo, con
 * valores <em>distintos</em>: si el adaptador leyera la misma dos veces, con
 * valores iguales el test pasaria igual.</li>
 * <li><b>{@code setup_amount}</b>, que en un {@code ONE_TIME} es todo su
 * precio.</li>
 * </ul>
 *
 * <p>
 * <b>Todo lo que se afirma va acotado a la tarifa sembrada aqui o a los codigos
 * {@code TESTCAT_}.</b> El contenedor MySQL es unico para la suite y Liquibase
 * ya siembra el catalogo comercial real, asi que un conteo global seria un
 * fallo intermitente esperando. Los codigos llevan prefijo propio porque
 * {@code uq_catalog_items_code} es UNIQUE global y
 * {@code PublicPlanQueryPortIT} comparte contenedor con sus {@code TEST_}.
 *
 * <p>
 * <b>El adaptador no se declara como bean</b>, por lo mismo que
 * {@code PublicPlanQueryPortIT}: solo necesita un {@code EntityManager} y
 * anadirlo al {@code @Import} cambiaria la clave del
 * {@code MergedContextConfiguration} y costaria un arranque de contexto entero.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPublicCatalogQueryPort — el SQL del catalogo contratable contra MySQL real")
class PublicCatalogQueryPortIT extends AbstractDataJpaTest {

    private static final Long LISTA = 2700L;
    private static final Long LISTA_OTRA = 2701L;

    private static final Long PACK = 2710L;
    private static final Long MOD_EN_PACK = 2711L;
    private static final Long MOD_SUELTO = 2712L;
    private static final Long MOD_SOLO_MENSUAL = 2713L;
    private static final Long MOD_SIN_PRECIO = 2714L;
    private static final Long MOD_BORRADOR = 2715L;
    private static final Long MOD_RETIRADO = 2716L;
    private static final Long CAP_NUCLEO = 2717L;
    private static final Long ONE_TIME = 2718L;
    private static final Long MOD_DE_BAJA = 2719L;

    private static final String COD_PACK = "TESTCAT_PACK";
    private static final String COD_MOD_EN_PACK = "TESTCAT_EN_PACK";
    private static final String COD_MOD_SUELTO = "TESTCAT_SUELTO";
    private static final String COD_SOLO_MENSUAL = "TESTCAT_SOLO_MENSUAL";
    private static final String COD_CAP_NUCLEO = "TESTCAT_CAP_NUCLEO";
    private static final String COD_ONE_TIME = "TESTCAT_ONE_TIME";

    @PersistenceContext
    private EntityManager entityManager;

    private JpaPublicCatalogQueryPort port;

    @BeforeEach
    void sembrar() {
        port = new JpaPublicCatalogQueryPort(entityManager);
        SchemaSeed.seed(entityManager);

        tarifa(LISTA, "TESTCAT-LISTA", LocalDate.of(2026, 8, 1));
        tarifa(LISTA_OTRA, "TESTCAT-OTRA", LocalDate.of(2025, 1, 1));

        articulo(PACK, COD_PACK, "Pack de prueba", "BUNDLE", null, false, 1, 1, "ACTIVE",
                "NEVER_FREE", null, null);
        articulo(MOD_EN_PACK, COD_MOD_EN_PACK, "Modulo dentro del pack", "MODULE", null, false, 1,
                2, "ACTIVE", "ELIGIBLE", 30, "CONVERT_TO_PAID");
        articulo(MOD_SUELTO, COD_MOD_SUELTO, "Modulo de catalogo interno", "MODULE", null, false, 1,
                3, "ACTIVE", "NEVER_FREE", null, null);
        articulo(MOD_SOLO_MENSUAL, COD_SOLO_MENSUAL, "Modulo solo mensual", "MODULE", null, false,
                1, 4, "ACTIVE", "NEVER_FREE", null, null);
        articulo(MOD_SIN_PRECIO, "TESTCAT_SIN_PRECIO", "Modulo sin tarifar", "MODULE", null, false,
                1, 5, "ACTIVE", "NEVER_FREE", null, null);
        articulo(MOD_BORRADOR, "TESTCAT_BORRADOR", "Modulo en redaccion", "MODULE", null, false, 1,
                6, "DRAFT", "NEVER_FREE", null, null);
        articulo(MOD_RETIRADO, "TESTCAT_RETIRADO", "Modulo retirado", "MODULE", null, false, 1, 7,
                "DEPRECATED", "NEVER_FREE", null, null);
        articulo(CAP_NUCLEO, COD_CAP_NUCLEO, "Usuario incluido", "CAPACITY", "USER", true, 1, 8,
                "ACTIVE", "NEVER_FREE", null, null);
        articulo(ONE_TIME, COD_ONE_TIME, "Migracion de datos", "ONE_TIME", null, false, 1, 9,
                "ACTIVE", "NEVER_FREE", null, null);
        articuloDeBaja(MOD_DE_BAJA, "TESTCAT_DE_BAJA", "Modulo dado de baja");

        linea(2730L, PACK, MOD_EN_PACK, 1, true);

        precio(2740L, LISTA, PACK, "MONTHLY", 1, 10, 0, "89000.00", "150000.00", true);
        precio(2741L, LISTA, PACK, "MONTHLY", 11, null, 0, "79000.00", "0.00", true);
        precio(2742L, LISTA, PACK, "ANNUAL", 1, null, 0, "890000.00", "200000.00", true);

        precio(2743L, LISTA, MOD_EN_PACK, "MONTHLY", 1, null, 0, "38000.00", "0.00", true);
        precio(2744L, LISTA, MOD_EN_PACK, "ANNUAL", 1, null, 0, "350000.00", "0.00", true);

        precio(2745L, LISTA, MOD_SUELTO, "MONTHLY", 1, null, 0, "25000.00", "0.00", true);
        precio(2746L, LISTA, MOD_SOLO_MENSUAL, "MONTHLY", 1, null, 0, "29000.00", "0.00", true);

        precio(2747L, LISTA, CAP_NUCLEO, "MONTHLY", 1, null, 3, "15000.00", "0.00", true);
        precio(2748L, LISTA, CAP_NUCLEO, "ANNUAL", 1, null, 5, "145000.00", "0.00", true);

        precio(2749L, LISTA, ONE_TIME, "MONTHLY", 1, null, 0, "0.00", "450000.00", true);

        precio(2750L, LISTA, MOD_BORRADOR, "MONTHLY", 1, null, 0, "50000.00", "0.00", true);
        precio(2751L, LISTA, MOD_RETIRADO, "MONTHLY", 1, null, 0, "50000.00", "0.00", true);
        precio(2752L, LISTA, MOD_DE_BAJA, "MONTHLY", 1, null, 0, "50000.00", "0.00", true);
        precio(2753L, LISTA_OTRA, MOD_SIN_PRECIO, "MONTHLY", 1, null, 0, "99000.00", "0.00", true);
        entityManager.flush();
    }

    private List<PublicCatalogItemRowDto> articulosDePrueba() {
        return port.findContractableItems(LISTA).stream()
                .filter(fila -> fila.code().startsWith("TESTCAT_")).toList();
    }

    @Nested
    @DisplayName("que entra y que no")
    class Alcance {

        @Test
        @DisplayName("solo los articulos ACTIVE, habilitados y con precio en esa tarifa")
        void solo_los_activos_con_precio_en_esa_tarifa() {
            assertThat(articulosDePrueba()).extracting(PublicCatalogItemRowDto::code)
                    .containsExactlyInAnyOrder(COD_MOD_EN_PACK, COD_MOD_SUELTO, COD_SOLO_MENSUAL,
                            COD_CAP_NUCLEO, COD_ONE_TIME);
        }

        @Test
        @DisplayName("el paquete no sale entre los articulos sueltos: lo sirve findPacks")
        void el_paquete_no_sale_entre_los_sueltos() {
            assertThat(articulosDePrueba()).extracting(PublicCatalogItemRowDto::code)
                    .doesNotContain(COD_PACK);
        }
    }

    @Nested
    @DisplayName("lo que un mock no puede comprobar")
    class ColumnasDeVerdad {

        /**
         * MySQL entrega {@code TINYINT} como {@code Byte}. Si el adaptador hiciera un
         * cast directo a {@code Boolean}, esto revienta con {@code ClassCastException};
         * si comparase mal, {@code mandatory} saldria invertido.
         */
        @Test
        @DisplayName("is_core llega como TINYINT y se convierte: el del nucleo sale obligatorio")
        void is_core_se_convierte_desde_tinyint() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_CAP_NUCLEO.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.mandatory()).isTrue());
            assertThat(articulosDePrueba()).filteredOn(f -> COD_MOD_SUELTO.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.mandatory()).isFalse());
        }

        /**
         * El mismo predicado que {@code JpaPublishedCatalogItemQueryPort}: colgar de un
         * paquete ACTIVE publicado. El modulo suelto tiene precio y no cuelga de
         * ninguno, asi que la contratacion lo rechazaria y aqui sale marcado.
         */
        @Test
        @DisplayName("selfServiceEligible es el EXISTS del gate, no una etiqueta")
        void self_service_eligible_es_el_exists_del_gate() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_MOD_EN_PACK.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.selfServiceEligible()).isTrue());
            assertThat(articulosDePrueba()).filteredOn(f -> COD_MOD_SUELTO.equals(f.code()))
                    .singleElement().satisfies(f -> assertThat(f.selfServiceEligible()).isFalse());
        }

        @Test
        @DisplayName("las dos included_quantity son columnas distintas, una por ciclo")
        void las_dos_included_quantity_son_distintas() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_CAP_NUCLEO.equals(f.code()))
                    .singleElement().satisfies(f -> {
                        assertThat(f.monthlyIncludedQuantity()).isEqualTo(3);
                        assertThat(f.annualIncludedQuantity()).isEqualTo(5);
                        assertThat(f.capacityUnit()).isEqualTo("USER");
                    });
        }

        /**
         * Sin esta columna el catalogo anunciaria la migracion de datos como gratuita:
         * su {@code unit_amount} es cero en los dos ciclos.
         */
        @Test
        @DisplayName("el cargo unico publica su precio real, que vive en setup_amount")
        void el_cargo_unico_publica_su_setup_amount() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_ONE_TIME.equals(f.code()))
                    .singleElement().satisfies(f -> {
                        assertThat(f.monthlyAmount()).isEqualByComparingTo("0.00");
                        assertThat(f.setupAmount()).isEqualByComparingTo("450000.00");
                        assertThat(f.selfServiceEligible()).isFalse();
                    });
        }

        @Test
        @DisplayName("un modulo sin tarifa anual publica null, no un cero ni el mensual")
        void un_modulo_sin_tarifa_anual_publica_null() {
            assertThat(articulosDePrueba()).filteredOn(f -> COD_SOLO_MENSUAL.equals(f.code()))
                    .singleElement().satisfies(f -> {
                        assertThat(f.monthlyAmount()).isEqualByComparingTo("29000.00");
                        assertThat(f.annualAmount()).isNull();
                    });
        }
    }

    @Nested
    @DisplayName("paquetes y composicion")
    class Paquetes {

        @Test
        @DisplayName("del paquete sale el tramo de entrada, no la escalera por volumen")
        void del_paquete_sale_el_tramo_de_entrada() {
            assertThat(port.findPacks(LISTA)).filteredOn(p -> COD_PACK.equals(p.code()))
                    .singleElement().satisfies(p -> {
                        assertThat(p.monthlyFromAmount()).isEqualByComparingTo("89000.00");
                        assertThat(p.annualFromAmount()).isEqualByComparingTo("890000.00");
                        assertThat(p.setupAmount()).isEqualByComparingTo("150000.00");
                        assertThat(p.taxTreatment()).isEqualTo(TaxTreatment.TAXED);
                    });
        }

        @Test
        @DisplayName("la composicion sale por rotulos: es el grafo del rechazo de cobro doble")
        void la_composicion_sale_por_rotulos() {
            assertThat(port.findPackComponents(LISTA))
                    .filteredOn(c -> COD_PACK.equals(c.packCode()))
                    .extracting(PublicCatalogPackComponentRowDto::componentCode)
                    .containsExactly(COD_MOD_EN_PACK);
        }

        @Test
        @DisplayName("sin tarifa no hay nada que publicar")
        void sin_tarifa_no_hay_nada_que_publicar() {
            assertThat(port.findContractableItems(null)).isEmpty();
            assertThat(port.findPacks(null)).isEmpty();
            assertThat(port.findPackComponents(null)).isEmpty();
        }
    }

    private void tarifa(Long id, String code, LocalDate desde) {
        entityManager.createNativeQuery("""
                INSERT INTO price_lists (id, code, name, currency, valid_from, valid_to, status,
                                         published_at, published_by_system_user_id,
                                         created_date, enabled, version)
                VALUES (:id, :code, :code, 'COP', :desde, NULL, 'PUBLISHED', :firmadoEl,
                        :firmante, '2026-01-01 00:00:00', TRUE, 0)
                """).setParameter("id", id).setParameter("code", code).setParameter("desde", desde)
                .setParameter("firmadoEl", LocalDateTime.of(2026, 1, 1, 0, 0))
                .setParameter("firmante", SchemaSeed.SYSTEM_USER_ID).executeUpdate();
    }

    private void articulo(Long id, String code, String name, String itemType, String capacityUnit,
            boolean core, int minQuantity, int sortOrder, String status, String trialEligibility,
            Integer trialDays, String trialOutcome) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_items (id, code, name, short_description, item_type,
                                           capacity_unit, is_core, min_quantity, max_quantity,
                                           sort_order, status, trial_eligibility,
                                           default_trial_days, trial_outcome, service_nature,
                                           created_date, enabled, version)
                VALUES (:id, :code, :name, NULL, :itemType, :capacityUnit, :core, :minQuantity,
                        NULL, :sortOrder, :status, :elegibilidad, :dias, :desenlace,
                        'SOFTWARE_LICENSING', '2026-01-01 00:00:00', TRUE, 0)
                """).setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .setParameter("itemType", itemType).setParameter("capacityUnit", capacityUnit)
                .setParameter("core", core).setParameter("minQuantity", minQuantity)
                .setParameter("sortOrder", sortOrder).setParameter("status", status)
                .setParameter("elegibilidad", trialEligibility).setParameter("dias", trialDays)
                .setParameter("desenlace", trialOutcome).executeUpdate();
    }

    private void articuloDeBaja(Long id, String code, String name) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_items (id, code, name, short_description, item_type,
                                           capacity_unit, is_core, min_quantity, max_quantity,
                                           sort_order, status, trial_eligibility,
                                           default_trial_days, trial_outcome, service_nature,
                                           created_date, enabled, version)
                VALUES (:id, :code, :name, NULL, 'MODULE', NULL, FALSE, 1, NULL, 20, 'ACTIVE',
                        'NEVER_FREE', NULL, NULL, 'SOFTWARE_LICENSING',
                        '2026-01-01 00:00:00', FALSE, 0)
                """).setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .executeUpdate();
    }

    private void linea(Long id, Long bundleId, Long componentId, int quantity, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO bundle_components (id, bundle_item_id, component_item_id, quantity,
                                               created_date, enabled)
                VALUES (:id, :paquete, :componente, :cantidad, '2026-01-01 00:00:00', :enabled)
                """).setParameter("id", id).setParameter("paquete", bundleId)
                .setParameter("componente", componentId).setParameter("cantidad", quantity)
                .setParameter("enabled", enabled).executeUpdate();
    }

    private void precio(Long id, Long priceListId, Long catalogItemId, String cycle, int tierMin,
            Integer tierMax, int includedQuantity, String unitAmount, String setupAmount,
            boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_prices (id, price_list_id, catalog_item_id, billing_cycle,
                                            tier_min, tier_max, included_quantity, unit_amount,
                                            setup_amount, tax_rate, tax_treatment,
                                            created_date, enabled, version)
                VALUES (:id, :lista, :articulo, :ciclo, :tierMin, :tierMax, :incluidas, :importe,
                        :implantacion, 19.00, 'TAXED', '2026-01-01 00:00:00', :enabled, 0)
                """).setParameter("id", id).setParameter("lista", priceListId)
                .setParameter("articulo", catalogItemId).setParameter("ciclo", cycle)
                .setParameter("tierMin", tierMin).setParameter("tierMax", tierMax)
                .setParameter("incluidas", includedQuantity)
                .setParameter("importe", new BigDecimal(unitAmount))
                .setParameter("implantacion", new BigDecimal(setupAmount))
                .setParameter("enabled", enabled).executeUpdate();
    }
}
