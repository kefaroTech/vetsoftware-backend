package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.aiproposal.domain.PackOffer;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.domain.SellableItem;
import com.vetsoftware.app.aiproposal.domain.SellableItemKind;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * La rodaja que {@link JpaSellableCatalogQueryPort} y
 * {@link JpaCatalogHintQueryPort} necesitan y que <b>ninguna regla de ArchUnit
 * exige</b>.
 *
 * <p>
 * <b>Por que hay que escribirla a mano.</b> {@code ADAPTADOR_JPA_CON_RODAJA}
 * solo alcanza a las clases que se llaman {@code Jpa<Algo>Repository}; estas
 * dos terminan en {@code QueryPort} y la regla ni las mira. Un inventario del
 * arbol cuenta 20 adaptadores con consulta nativa y cinco sin ninguna
 * cobertura, y es exactamente asi como la incidencia #196 sobrevivio meses:
 * nadie ejecutaba su SQL hasta produccion.
 *
 * <p>
 * <b>Lo que se fija aqui, y ningun mock puede</b>:
 *
 * <ul>
 * <li>⛔ <b>Que la escalera de precios se lea ENTERA.</b> Es la prueba de D-66 a
 * nivel de SQL, y esta construida para que un {@code AND p.tier_min = 1} —el
 * filtro que llevan los otros cuatro adaptadores de estas tablas, y con razon,
 * porque publican un precio "desde"— haga desaparecer el articulo del catalogo
 * y ponga el test en rojo. Comprobar solo el importe no serviria: para una
 * unidad, el tramo de entrada y la escalera completa dan lo mismo.</li>
 * <li><b>Que {@code is_core} llegue como {@code Byte} y no como
 * {@code Boolean}</b>. MySQL entrega {@code TINYINT} asi y nadie lo convierte
 * solo: es la clase de defecto de #472, que tumbo el alta de empresa
 * entera.</li>
 * <li><b>Que el {@code EXISTS} del autoservicio de el MISMO veredicto</b> que
 * el gate de la contratacion. Si divergieran, la propuesta cotizaria lineas que
 * el paso 6 rechaza con un texto indistinguible, despues de que el prospecto se
 * registro y verifico el correo.</li>
 * <li><b>Que los componentes del paquete sean SOLO {@code MODULE}</b>, que es
 * la correccion de S1.5: con los {@code CAPACITY} dentro, la contencion no se
 * cumplia nunca y la comparacion era codigo muerto.</li>
 * </ul>
 *
 * <p>
 * <b>Todo lo que se afirma va acotado a la tarifa sembrada aqui o a los codigos
 * {@code TESTAI_}.</b> El contenedor MySQL es unico para la suite y Liquibase
 * ya siembra el catalogo comercial real (308-313); un conteo global seria un
 * fallo intermitente esperando. El prefijo es propio porque
 * {@code uq_catalog_items_code} es UNIQUE global y otras rodajas comparten
 * contenedor con sus {@code TEST_} y {@code TESTCAT_}.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSellableCatalogQueryPort — el SQL que cotiza, contra MySQL real")
class SellableCatalogQueryPortIT extends AbstractDataJpaTest {

    private static final Long LISTA = 2800L;
    private static final Long LISTA_CADUCADA = 2801L;
    private static final Long LISTA_BORRADOR = 2802L;

    private static final Long PACK = 2810L;
    private static final Long MOD_EN_PACK = 2811L;
    private static final Long MOD_FUERA_DE_PACK = 2812L;
    private static final Long MOD_BORRADOR = 2813L;
    private static final Long MOD_RETIRADO = 2814L;
    private static final Long CAP_EN_PACK = 2815L;
    private static final Long ESCALERA = 2816L;
    private static final Long ESCALERA_ROTA = 2817L;

    private static final String COD_PACK = "TESTAI_PACK";
    private static final String COD_EN_PACK = "TESTAI_EN_PACK";
    private static final String COD_FUERA = "TESTAI_FUERA";
    private static final String COD_BORRADOR = "TESTAI_BORRADOR";
    private static final String COD_RETIRADO = "TESTAI_RETIRADO";
    private static final String COD_CAP = "TESTAI_CAP";
    private static final String COD_ESCALERA = "TESTAI_ESCALERA";
    private static final String COD_ROTA = "TESTAI_ROTA";

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-08-30T10:00:00Z"),
            ZoneOffset.UTC);

    @PersistenceContext
    private EntityManager entityManager;

    private JpaSellableCatalogQueryPort port;

    private JpaCatalogHintQueryPort hintPort;

    @BeforeEach
    void sembrar() {
        port = new JpaSellableCatalogQueryPort(entityManager, RELOJ);
        hintPort = new JpaCatalogHintQueryPort(entityManager);
        SchemaSeed.seed(entityManager);

        tarifa(LISTA, "TESTAI-LISTA", LocalDate.of(2026, 8, 1), null, "PUBLISHED");
        tarifa(LISTA_CADUCADA, "TESTAI-VIEJA", LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1),
                "PUBLISHED");
        tarifa(LISTA_BORRADOR, "TESTAI-DRAFT", LocalDate.of(2026, 8, 1), null, "DRAFT");

        articulo(PACK, COD_PACK, "Pack de prueba", "BUNDLE", null, false, 1, "ACTIVE", "NEVER_FREE",
                null);
        articulo(MOD_EN_PACK, COD_EN_PACK, "Modulo dentro del pack", "MODULE", null, true, 2,
                "ACTIVE", "ELIGIBLE", 30);
        articulo(MOD_FUERA_DE_PACK, COD_FUERA, "Modulo suelto", "MODULE", null, false, 3, "ACTIVE",
                "NEVER_FREE", null);
        articulo(MOD_BORRADOR, COD_BORRADOR, "Modulo en redaccion", "MODULE", null, false, 4,
                "DRAFT", "NEVER_FREE", null);
        articulo(MOD_RETIRADO, COD_RETIRADO, "Modulo retirado", "MODULE", null, false, 5,
                "DEPRECATED", "NEVER_FREE", null);
        articulo(CAP_EN_PACK, COD_CAP, "Terminal incluida", "CAPACITY", "TERMINAL", false, 6,
                "ACTIVE", "NEVER_FREE", null);
        articulo(ESCALERA, COD_ESCALERA, "Con escalera de dos tramos", "CAPACITY", "USER", false, 7,
                "ACTIVE", "NEVER_FREE", null);
        articulo(ESCALERA_ROTA, COD_ROTA, "Con escalera con hueco", "MODULE", null, false, 8,
                "ACTIVE", "NEVER_FREE", null);

        // El pack trae un MODULE y una CAPACITY: solo el primero es componente a
        // efectos de la comparacion (S1.5).
        componente(2830L, PACK, MOD_EN_PACK);
        componente(2831L, PACK, CAP_EN_PACK);

        requisito(2840L, MOD_EN_PACK, MOD_FUERA_DE_PACK, "REQUIRES");
        requisito(2841L, MOD_FUERA_DE_PACK, MOD_EN_PACK, "RECOMMENDS");

        precio(2850L, LISTA, PACK, 1, null, 0, "189000.00");
        precio(2851L, LISTA, MOD_EN_PACK, 1, null, 0, "69000.00");
        precio(2852L, LISTA, MOD_FUERA_DE_PACK, 1, null, 0, "35000.00");
        precio(2853L, LISTA, MOD_BORRADOR, 1, null, 0, "50000.00");
        precio(2854L, LISTA, MOD_RETIRADO, 1, null, 0, "50000.00");
        precio(2855L, LISTA, CAP_EN_PACK, 1, null, 0, "0.00");

        // ⛔ La escalera de EXTRA_USER, con sus importes reales de la semilla 310.
        // Si el adaptador filtrara por tier_min = 1 leeria SOLO el tramo 1-8, y un
        // ultimo tramo cerrado hace que PriceLadder rechace la escalera entera: el
        // articulo desapareceria del catalogo y el test de abajo se pone rojo.
        precio(2856L, LISTA, ESCALERA, 1, 8, 0, "12000.00");
        precio(2857L, LISTA, ESCALERA, 9, null, 0, "9000.00");

        // Una escalera con hueco: unidades 6-8 sin precio. No se cotiza.
        precio(2858L, LISTA, ESCALERA_ROTA, 1, 5, 0, "10000.00");
        precio(2859L, LISTA, ESCALERA_ROTA, 9, null, 0, "8000.00");

        precio(2860L, LISTA_CADUCADA, MOD_FUERA_DE_PACK, 1, null, 0, "99000.00");
        entityManager.flush();
    }

    private SellableCatalog catalogo() {
        return port.loadCatalog(LISTA, ProposalBillingCycle.MONTHLY).orElseThrow();
    }

    private SellableItem articuloDePrueba(String code) {
        return catalogo().find(code).orElseThrow();
    }

    @Nested
    @DisplayName("La escalera de precios — la prueba de D-66 en SQL")
    class Escalera {

        @Test
        @DisplayName("un articulo con dos tramos SE COTIZA: se leyeron los dos")
        void los_dos_tramos_se_leen() {
            // Con `AND p.tier_min = 1` en la consulta, el ultimo tramo visible seria
            // 1-8 (cerrado), PriceLadder lo rechazaria y este articulo no estaria.
            assertThat(catalogo().find(COD_ESCALERA)).isPresent();
        }

        @Test
        @DisplayName("y su precio de una unidad es el del primer tramo")
        void el_precio_de_una_unidad() {
            assertThat(articuloDePrueba(COD_ESCALERA).unitAmount())
                    .isEqualByComparingTo("12000.00");
        }

        @Test
        @DisplayName("una escalera con hueco NO se cotiza: se omite en vez de cobrar mal")
        void la_escalera_rota_se_omite() {
            assertThat(catalogo().find(COD_ROTA)).isEmpty();
        }

        @Test
        @DisplayName("el impuesto es por articulo, leido de catalog_prices")
        void el_impuesto_es_por_articulo() {
            assertThat(articuloDePrueba(COD_EN_PACK).taxRate()).isEqualByComparingTo("19.00");
        }

        @Test
        @DisplayName("la divisa viaja siempre, y sale de la tarifa")
        void la_divisa_viaja() {
            assertThat(articuloDePrueba(COD_EN_PACK).currency()).isEqualTo("COP");
        }
    }

    @Nested
    @DisplayName("El gate del autoservicio")
    class Autoservicio {

        @Test
        @DisplayName("un modulo que cuelga de un pack publicado SI se vende")
        void el_que_cuelga_del_pack() {
            assertThat(articuloDePrueba(COD_EN_PACK).selfServiceEligible()).isTrue();
        }

        @Test
        @DisplayName("uno que no cuelga de ninguno NO se vende: es la DC-1 en datos")
        void el_que_no_cuelga() {
            assertThat(articuloDePrueba(COD_FUERA).selfServiceEligible()).isFalse();
            assertThat(articuloDePrueba(COD_FUERA).esCotizable()).isFalse();
        }

        @Test
        @DisplayName("un BUNDLE se vende siempre por serlo")
        void el_paquete_se_vende() {
            assertThat(articuloDePrueba(COD_PACK).selfServiceEligible()).isTrue();
        }
    }

    @Nested
    @DisplayName("Lo que NO se filtra, y por que")
    class NoSeFiltra {

        @Test
        @DisplayName("un borrador llega marcado inactivo, para dar NOT_SELLABLE")
        void el_borrador_llega() {
            SellableItem borrador = articuloDePrueba(COD_BORRADOR);

            // Sin el, el motor no distingue "ese codigo no existe" de "existe y no se
            // publica": dos veredictos que juntos miden la calidad del modelo.
            assertThat(borrador.active()).isFalse();
            assertThat(borrador.esCotizable()).isFalse();
        }

        @Test
        @DisplayName("un retirado tambien")
        void el_retirado_llega() {
            assertThat(articuloDePrueba(COD_RETIRADO).active()).isFalse();
        }
    }

    @Nested
    @DisplayName("Los paquetes y sus componentes")
    class Paquetes {

        private PackOffer packDePrueba() {
            return catalogo().packs().stream().filter(pack -> COD_PACK.equals(pack.code()))
                    .findFirst().orElseThrow();
        }

        @Test
        @DisplayName("solo los componentes MODULE entran en la contencion")
        void solo_los_modulos() {
            // Con la CAPACITY dentro, "el paquete esta contenido en el carrito" no se
            // cumpliria nunca y la comparacion seria codigo muerto (S1.5).
            assertThat(packDePrueba().moduleComponentCodes()).containsExactly(COD_EN_PACK)
                    .doesNotContain(COD_CAP);
        }

        @Test
        @DisplayName("el paquete lleva su precio y es comparable")
        void el_paquete_tiene_precio() {
            assertThat(packDePrueba().unitAmount()).isEqualByComparingTo("189000.00");
            assertThat(packDePrueba().esComparable()).isTrue();
        }

        @Test
        @DisplayName("y el articulo del pack se clasifica como BUNDLE")
        void el_tipo_cruza_bien() {
            assertThat(articuloDePrueba(COD_PACK).kind()).isEqualTo(SellableItemKind.BUNDLE);
            assertThat(articuloDePrueba(COD_CAP).kind()).isEqualTo(SellableItemKind.CAPACITY);
            assertThat(articuloDePrueba(COD_EN_PACK).kind()).isEqualTo(SellableItemKind.MODULE);
        }
    }

    @Nested
    @DisplayName("Los arcos y las banderas")
    class ArcosYBanderas {

        @Test
        @DisplayName("solo se publican los REQUIRES: un RECOMMENDS no se puede cerrar")
        void solo_requires() {
            assertThat(catalogo().requiredBy(COD_EN_PACK)).containsExactly(COD_FUERA);
            assertThat(catalogo().requiredBy(COD_FUERA)).isEmpty();
        }

        @Test
        @DisplayName("is_core llega como booleano aunque MySQL lo entregue como Byte")
        void el_tinyint_se_convierte() {
            assertThat(articuloDePrueba(COD_EN_PACK).core()).isTrue();
            assertThat(articuloDePrueba(COD_FUERA).core()).isFalse();
        }

        @Test
        @DisplayName("los dias de prueba salen del CASE sobre trial_eligibility")
        void los_dias_de_prueba() {
            assertThat(articuloDePrueba(COD_EN_PACK).trialDays()).isEqualTo(30);
            assertThat(articuloDePrueba(COD_FUERA).trialDays()).isZero();
        }
    }

    @Nested
    @DisplayName("La tarifa vigente")
    class Tarifa {

        @Test
        @DisplayName("se elige una publicada y dentro de su ventana")
        void la_vigente() {
            Optional<Long> vigente = port.findPublishedPriceListId();

            assertThat(vigente).isPresent();
            assertThat(vigente.get()).isNotEqualTo(LISTA_CADUCADA).isNotEqualTo(LISTA_BORRADOR);
        }

        @Test
        @DisplayName("una lista sin precios no produce catalogo")
        void sin_precios_no_hay_catalogo() {
            assertThat(port.loadCatalog(LISTA_BORRADOR, ProposalBillingCycle.MONTHLY)).isEmpty();
        }

        @Test
        @DisplayName("sin lista o sin ciclo devuelve vacio en vez de reventar")
        void los_nulos_devuelven_vacio() {
            assertThat(port.loadCatalog(null, ProposalBillingCycle.MONTHLY)).isEmpty();
            assertThat(port.loadCatalog(LISTA, null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Los hints del prompt")
    class Hints {

        @Test
        @DisplayName("sin filas sembradas la feature es muda, y eso es LEGITIMO")
        void muda_por_defecto() {
            // El changeset 382 no inserta nada si system_users esta vacia, que es el
            // estado de toda base recien migrada, esta incluida.
            assertThat(hintPort.findCurrentHints()).doesNotContainKey(COD_EN_PACK);
        }

        @Test
        @DisplayName("la revision vigente se lee; la superada no")
        void solo_la_vigente() {
            hint(2870L, MOD_EN_PACK, 1, "Texto viejo que ya no se usa", true);
            hint(2871L, MOD_EN_PACK, 2, "Para quien atiende medicamente.", false);
            entityManager.flush();

            Map<String, String> hints = hintPort.findCurrentHints();

            assertThat(hints).containsEntry(COD_EN_PACK, "Para quien atiende medicamente.");
            assertThat(hints.get(COD_EN_PACK)).isNotEqualTo("Texto viejo que ya no se usa");
        }

        @Test
        @DisplayName("el hint de un articulo retirado no entra al prompt")
        void el_hint_de_un_retirado_no_entra() {
            hint(2872L, MOD_RETIRADO, 1, "No deberia llegar al modelo", false);
            entityManager.flush();

            assertThat(hintPort.findCurrentHints()).doesNotContainKey(COD_RETIRADO);
        }
    }

    private void tarifa(Long id, String code, LocalDate desde, LocalDate hasta, String status) {
        entityManager.createNativeQuery("""
                INSERT INTO price_lists (id, code, name, currency, valid_from, valid_to, status,
                                         published_at, published_by_system_user_id,
                                         created_date, enabled, version)
                VALUES (:id, :code, :code, 'COP', :desde, :hasta, :status, :firmadoEl,
                        :firmante, '2026-01-01 00:00:00', TRUE, 0)
                """).setParameter("id", id).setParameter("code", code).setParameter("desde", desde)
                .setParameter("hasta", hasta).setParameter("status", status)
                .setParameter("firmadoEl",
                        "DRAFT".equals(status) ? null : LocalDateTime.of(2026, 1, 1, 0, 0))
                .setParameter("firmante", "DRAFT".equals(status) ? null : SchemaSeed.SYSTEM_USER_ID)
                .executeUpdate();
    }

    private void articulo(Long id, String code, String name, String itemType, String capacityUnit,
            boolean core, int sortOrder, String status, String trialEligibility,
            Integer trialDays) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_items (id, code, name, short_description, item_type,
                                           capacity_unit, is_core, min_quantity, max_quantity,
                                           sort_order, status, trial_eligibility,
                                           default_trial_days, trial_outcome, service_nature,
                                           created_date, enabled, version)
                VALUES (:id, :code, :name, :descripcion, :itemType, :capacityUnit, :core, 1,
                        NULL, :sortOrder, :status, :elegibilidad, :dias, :desenlace,
                        'SOFTWARE_LICENSING', '2026-01-01 00:00:00', TRUE, 0)
                """).setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .setParameter("descripcion", "Descripcion de " + name)
                .setParameter("itemType", itemType).setParameter("capacityUnit", capacityUnit)
                .setParameter("core", core).setParameter("sortOrder", sortOrder)
                .setParameter("status", status).setParameter("elegibilidad", trialEligibility)
                .setParameter("dias", trialDays)
                .setParameter("desenlace", trialDays == null ? null : "CONVERT_TO_PAID")
                .executeUpdate();
    }

    private void componente(Long id, Long bundleId, Long componentId) {
        entityManager.createNativeQuery("""
                INSERT INTO bundle_components (id, bundle_item_id, component_item_id, quantity,
                                               created_date, enabled)
                VALUES (:id, :paquete, :componente, 1, '2026-01-01 00:00:00', TRUE)
                """).setParameter("id", id).setParameter("paquete", bundleId)
                .setParameter("componente", componentId).executeUpdate();
    }

    private void requisito(Long id, Long itemId, Long relatedId, String tipo) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_item_dependencies (id, catalog_item_id, related_item_id,
                                                       relation_type, note, created_date, enabled)
                VALUES (:id, :articulo, :relacionado, :tipo, NULL, '2026-01-01 00:00:00', TRUE)
                """).setParameter("id", id).setParameter("articulo", itemId)
                .setParameter("relacionado", relatedId).setParameter("tipo", tipo).executeUpdate();
    }

    private void precio(Long id, Long priceListId, Long catalogItemId, int tierMin, Integer tierMax,
            int includedQuantity, String unitAmount) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_prices (id, price_list_id, catalog_item_id, billing_cycle,
                                            tier_min, tier_max, included_quantity, unit_amount,
                                            setup_amount, tax_rate, tax_treatment,
                                            created_date, enabled, version)
                VALUES (:id, :lista, :articulo, 'MONTHLY', :tierMin, :tierMax, :incluidas,
                        :importe, 0.00, 19.00, 'TAXED', '2026-01-01 00:00:00', TRUE, 0)
                """).setParameter("id", id).setParameter("lista", priceListId)
                .setParameter("articulo", catalogItemId).setParameter("tierMin", tierMin)
                .setParameter("tierMax", tierMax).setParameter("incluidas", includedQuantity)
                .setParameter("importe", new BigDecimal(unitAmount)).executeUpdate();
    }

    /**
     * {@code hint_hash} y {@code current_hint_marker} son {@code GENERATED ALWAYS}:
     * MySQL devuelve {@code ERROR 3105} si se nombran en el {@code INSERT}, aunque
     * el valor sea {@code NULL}. Estan deliberadamente ausentes.
     */
    private void hint(Long id, Long catalogItemId, int revision, String texto, boolean superada) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_item_ai_hints (id, catalog_item_id, hint_revision, hint_text,
                                                   published_at, published_by_system_user_id,
                                                   superseded_at, created_date, version)
                VALUES (:id, :articulo, :revision, :texto, '2026-01-01 00:00:00', :firmante,
                        :superada, '2026-01-01 00:00:00', 0)
                """).setParameter("id", id).setParameter("articulo", catalogItemId)
                .setParameter("revision", revision).setParameter("texto", texto)
                .setParameter("firmante", SchemaSeed.SYSTEM_USER_ID)
                .setParameter("superada", superada ? LocalDateTime.of(2026, 2, 1, 0, 0) : null)
                .executeUpdate();
    }
}
