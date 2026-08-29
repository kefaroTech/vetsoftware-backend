package com.vetsoftware.app.configurator.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.configurator.domain.BillingCycle;
import com.vetsoftware.app.configurator.domain.CatalogItemRef;
import com.vetsoftware.app.configurator.domain.PublishedPriceListRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Las dos consultas nativas que el configurador estrena, contra MySQL real.
 *
 * <p>
 * <b>Ninguna de las dos la alcanza {@code ADAPTADOR_JPA_CON_RODAJA}</b> —esa
 * regla solo mira los {@code Jpa<Algo>Repository}—, asi que sin esta clase se
 * quedarian probadas solo con mocks. Y no son consultas decorativas: entre las
 * dos deciden <b>que rotulo se publica</b> y <b>cuantas unidades se le cobran a
 * alguien</b>.
 *
 * <p>
 * Lo que se fija es lo que un mock devuelve de mentira:
 *
 * <ul>
 * <li><b>{@code is_core} viaja como {@code TINYINT}</b> y MySQL lo entrega como
 * {@code Byte}. Es la clase de defecto de #472. Aqui hay un articulo del nucleo
 * y uno que no, y la distincion decide a cual se le resta el techo.</li>
 * <li><b>El techo es {@code included_quantity + min_quantity}</b>, dos columnas
 * de dos tablas distintas. Se siembran con valores que no se puedan confundir
 * (2 + 3 = 5) para que sumar mal, o leer una sola, se note.</li>
 * <li><b>El techo depende del ciclo.</b> Los dos ciclos llevan
 * {@code included_quantity} distinto a proposito: con el ciclo clavado en
 * {@code MONTHLY} —como estuvo— el caso anual falla.</li>
 * <li><b>El filtro por estado.</b> Un articulo retirado no tiene rotulo que
 * publicar, y el caso de uso lo descarta del carrito en vez de sacarlo con el
 * codigo vacio.</li>
 * </ul>
 *
 * <p>
 * Codigos con prefijo {@code TESTCFG_} e ids propios: el contenedor MySQL es
 * unico para la suite, {@code uq_catalog_items_code} es UNIQUE global y hay
 * otras dos rodajas sembrando catalogo en el mismo contenedor.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("Adaptadores de catalogo del configurador — SQL nativo contra MySQL real")
class ConfiguratorCatalogQueryPortsIT extends AbstractDataJpaTest {

    private static final Long LISTA = 2800L;
    private static final Long LISTA_FUTURA = 2801L;

    private static final Long CAP_NUCLEO_USER = 2810L;
    private static final Long EXTRA_USER = 2811L;
    private static final Long MODULO = 2812L;
    private static final Long RETIRADO = 2813L;
    private static final Long DE_BAJA = 2814L;
    private static final Long CAP_NO_NUCLEO = 2815L;

    private static final String COD_CAP_NUCLEO_USER = "TESTCFG_CAP_USER";
    private static final String COD_EXTRA_USER = "TESTCFG_EXTRA_USER";
    private static final String COD_MODULO = "TESTCFG_MODULO";
    private static final String COD_RETIRADO = "TESTCFG_RETIRADO";

    @PersistenceContext
    private EntityManager entityManager;

    private JpaCatalogItemQueryPort itemPort;
    private JpaCapacityCeilingQueryPort ceilingPort;

    @BeforeEach
    void sembrar() {
        itemPort = new JpaCatalogItemQueryPort(entityManager);
        ceilingPort = new JpaCapacityCeilingQueryPort(entityManager);
        SchemaSeed.seed(entityManager);

        tarifa(LISTA, "TESTCFG-LISTA", LocalDate.of(2026, 8, 1));
        tarifa(LISTA_FUTURA, "TESTCFG-FUTURA", LocalDate.of(2027, 1, 1));

        // is_core = TRUE y min_quantity = 3: el techo mensual sera 2 + 3 = 5.
        articulo(CAP_NUCLEO_USER, COD_CAP_NUCLEO_USER, "Usuario incluido", "CAPACITY", "USER", true,
                3, 1, "ACTIVE");
        articulo(EXTRA_USER, COD_EXTRA_USER, "Usuario adicional", "CAPACITY", "USER", false, 0, 2,
                "ACTIVE");
        articulo(MODULO, COD_MODULO, "Agenda", "MODULE", null, false, 1, 3, "ACTIVE");
        articulo(RETIRADO, COD_RETIRADO, "Modulo retirado", "MODULE", null, false, 1, 4,
                "DEPRECATED");
        articulo(CAP_NO_NUCLEO, "TESTCFG_CAP_TERMINAL", "Terminal adicional", "CAPACITY",
                "TERMINAL", false, 0, 5, "ACTIVE");
        articuloDeBaja(DE_BAJA, "TESTCFG_DE_BAJA", "Modulo de baja");

        // included_quantity DISTINTO por ciclo: mensual 2 (techo 5), anual 7 (techo
        // 10).
        precio(2820L, LISTA, CAP_NUCLEO_USER, "MONTHLY", 2, "0.00");
        precio(2821L, LISTA, CAP_NUCLEO_USER, "ANNUAL", 7, "0.00");
        // Un contador NO nuclear con precio: no debe aparecer como techo de su eje.
        precio(2822L, LISTA, CAP_NO_NUCLEO, "MONTHLY", 99, "18000.00");
        precio(2823L, LISTA, EXTRA_USER, "MONTHLY", 0, "12000.00");
        entityManager.flush();
    }

    @Nested
    @DisplayName("JpaCatalogItemQueryPort — traducir ids a rotulos")
    class Traduccion {

        @Test
        @DisplayName("devuelve rotulo, eje y nucleo de cada articulo activo")
        void devuelve_rotulo_eje_y_nucleo() {
            List<CatalogItemRef> refs = itemPort
                    .findActiveByIds(List.of(CAP_NUCLEO_USER, EXTRA_USER, MODULO));

            assertThat(refs).extracting(CatalogItemRef::code)
                    .containsExactlyInAnyOrder(COD_CAP_NUCLEO_USER, COD_EXTRA_USER, COD_MODULO);
        }

        /**
         * MySQL entrega {@code TINYINT} como {@code Byte}: un cast directo a
         * {@code Boolean} revienta aqui, y una comparacion mal escrita invierte el
         * significado. De ese booleano depende a que articulo se le resta el techo.
         */
        @Test
        @DisplayName("is_core llega como TINYINT y se convierte sin romperse")
        void is_core_se_convierte_desde_tinyint() {
            List<CatalogItemRef> refs = itemPort
                    .findActiveByIds(List.of(CAP_NUCLEO_USER, EXTRA_USER));

            assertThat(refs).filteredOn(r -> COD_CAP_NUCLEO_USER.equals(r.code())).singleElement()
                    .satisfies(r -> {
                        assertThat(r.core()).isTrue();
                        assertThat(r.esUnidadFacturable()).isFalse();
                    });
            assertThat(refs).filteredOn(r -> COD_EXTRA_USER.equals(r.code())).singleElement()
                    .satisfies(r -> {
                        assertThat(r.core()).isFalse();
                        assertThat(r.capacityUnit()).isEqualTo("USER");
                        assertThat(r.esUnidadFacturable()).isTrue();
                    });
        }

        @Test
        @DisplayName("un modulo no lleva eje: capacityUnit nulo y no es unidad facturable")
        void un_modulo_no_lleva_eje() {
            assertThat(itemPort.findActiveByIds(List.of(MODULO))).singleElement().satisfies(r -> {
                assertThat(r.capacityUnit()).isNull();
                assertThat(r.esContador()).isFalse();
            });
        }

        /**
         * El caso que introdujo el cambio de rotulos: un efecto puede apuntar a un
         * articulo retirado. No tiene rotulo que publicar, asi que se cae del carrito
         * en vez de salir con el codigo vacio.
         */
        @Test
        @DisplayName("un articulo retirado o de baja no se devuelve: se cae, no sale sin rotulo")
        void un_articulo_retirado_no_se_devuelve() {
            assertThat(itemPort.findActiveByIds(List.of(RETIRADO, DE_BAJA, MODULO)))
                    .extracting(CatalogItemRef::code).containsExactly(COD_MODULO)
                    .doesNotContain(COD_RETIRADO);
        }

        @Test
        @DisplayName("sin ids no se consulta la base")
        void sin_ids_no_se_consulta() {
            assertThat(itemPort.findActiveByIds(List.of())).isEmpty();
            assertThat(itemPort.findActiveByIds(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("JpaCapacityCeilingQueryPort — el techo por eje")
    class Techos {

        /**
         * {@code included_quantity} (2) del tramo de entrada mas {@code min_quantity}
         * (3) del articulo: dos columnas de dos tablas. Con 5 como resultado, leer solo
         * una de las dos da 2 o 3 y se nota.
         */
        @Test
        @DisplayName("el techo suma included_quantity del precio y min_quantity del articulo")
        void el_techo_suma_las_dos_columnas() {
            Map<String, Integer> techos = ceilingPort.findStructuralCeilingsByAxis(LISTA,
                    BillingCycle.MONTHLY);

            assertThat(techos).containsEntry("USER", 5);
        }

        /**
         * La prueba que se pone roja si alguien vuelve a clavar {@code MONTHLY} en la
         * consulta: el anual declara {@code included_quantity = 7}, luego techo 10.
         */
        @Test
        @DisplayName("el techo del ciclo anual es el suyo, no el del mensual")
        void el_techo_del_anual_es_el_suyo() {
            assertThat(ceilingPort.findStructuralCeilingsByAxis(LISTA, BillingCycle.ANNUAL))
                    .containsEntry("USER", 10);
        }

        /**
         * {@code is_core} se usa como predicado de conjunto. Un contador que no es del
         * nucleo tiene precio y eje, pero no aporta techo: si se colara, el eje
         * TERMINAL saldria con 99 y se regalarian noventa y nueve terminales.
         */
        @Test
        @DisplayName("un contador que no es del nucleo no aporta techo a su eje")
        void un_contador_no_nuclear_no_aporta_techo() {
            assertThat(ceilingPort.findStructuralCeilingsByAxis(LISTA, BillingCycle.MONTHLY))
                    .doesNotContainKey("TERMINAL");
        }

        @Test
        @DisplayName("una tarifa sin techos sembrados devuelve el mapa vacio, no un nulo")
        void una_tarifa_sin_techos_devuelve_vacio() {
            assertThat(ceilingPort.findStructuralCeilingsByAxis(LISTA_FUTURA, BillingCycle.MONTHLY))
                    .isEmpty();
        }

        @Test
        @DisplayName("sin tarifa o sin ciclo no se consulta la base")
        void sin_tarifa_o_sin_ciclo_no_se_consulta() {
            assertThat(ceilingPort.findStructuralCeilingsByAxis(null, BillingCycle.MONTHLY))
                    .isEmpty();
            assertThat(ceilingPort.findStructuralCeilingsByAxis(LISTA, null)).isEmpty();
        }

        /**
         * La ventana viaja hasta el caso de uso, que decide con
         * {@code PriceListValidity} sobre el reloj inyectado. El SQL no filtra por
         * fecha a proposito.
         */
        @Test
        @DisplayName("devuelve las publicadas con su ventana, sin filtrar por fecha")
        void devuelve_las_publicadas_con_su_ventana() {
            List<PublishedPriceListRef> listas = ceilingPort.findPublishedPriceLists();

            assertThat(listas).extracting(PublishedPriceListRef::id).contains(LISTA, LISTA_FUTURA);
            assertThat(listas).filteredOn(l -> LISTA_FUTURA.equals(l.id())).singleElement()
                    .satisfies(l -> {
                        assertThat(l.validFrom()).isEqualTo(LocalDate.of(2027, 1, 1));
                        assertThat(l.validTo()).isNull();
                    });
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
            boolean core, int minQuantity, int sortOrder, String status) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_items (id, code, name, short_description, item_type,
                                           capacity_unit, is_core, min_quantity, max_quantity,
                                           sort_order, status, trial_eligibility,
                                           default_trial_days, trial_outcome, service_nature,
                                           created_date, enabled, version)
                VALUES (:id, :code, :name, NULL, :itemType, :capacityUnit, :core, :minQuantity,
                        NULL, :sortOrder, :status, 'NEVER_FREE', NULL, NULL,
                        'SOFTWARE_LICENSING', '2026-01-01 00:00:00', TRUE, 0)
                """).setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .setParameter("itemType", itemType).setParameter("capacityUnit", capacityUnit)
                .setParameter("core", core).setParameter("minQuantity", minQuantity)
                .setParameter("sortOrder", sortOrder).setParameter("status", status)
                .executeUpdate();
    }

    private void articuloDeBaja(Long id, String code, String name) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_items (id, code, name, short_description, item_type,
                                           capacity_unit, is_core, min_quantity, max_quantity,
                                           sort_order, status, trial_eligibility,
                                           default_trial_days, trial_outcome, service_nature,
                                           created_date, enabled, version)
                VALUES (:id, :code, :name, NULL, 'MODULE', NULL, FALSE, 1, NULL, 30, 'ACTIVE',
                        'NEVER_FREE', NULL, NULL, 'SOFTWARE_LICENSING',
                        '2026-01-01 00:00:00', FALSE, 0)
                """).setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .executeUpdate();
    }

    private void precio(Long id, Long priceListId, Long catalogItemId, String cycle,
            int includedQuantity, String unitAmount) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_prices (id, price_list_id, catalog_item_id, billing_cycle,
                                            tier_min, tier_max, included_quantity, unit_amount,
                                            setup_amount, tax_rate, tax_treatment,
                                            created_date, enabled, version)
                VALUES (:id, :lista, :articulo, :ciclo, 1, NULL, :incluidas, :importe,
                        0.00, 19.00, 'TAXED', '2026-01-01 00:00:00', TRUE, 0)
                """).setParameter("id", id).setParameter("lista", priceListId)
                .setParameter("articulo", catalogItemId).setParameter("ciclo", cycle)
                .setParameter("incluidas", includedQuantity)
                .setParameter("importe", new BigDecimal(unitAmount)).executeUpdate();
    }
}
