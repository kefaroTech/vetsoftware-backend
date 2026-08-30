package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Los dos {@code INSERT ... SELECT ... ON DUPLICATE KEY UPDATE} que congelan la
 * composición de una línea al firmar (D-76), contra MySQL real.
 *
 * <p>
 * <b>Por qué esta rodaja hacía falta.</b> Hasta hoy el nombre
 * {@code JpaSubscriptionItemCompositionPort} <b>no aparecía en ninguna parte de
 * {@code src/test}</b>: su único ejercicio era un mock de Mockito en el test
 * del caso de uso, o sea un doble que devuelve lo que el propio test le dice
 * que devuelva. El contrato del puerto lo estaba definiendo el test, no el
 * esquema. Y queda fuera de {@code ADAPTADOR_JPA_CON_RODAJA}, que solo alcanza
 * a los {@code Jpa<Algo>Repository}, así que la red automática tampoco lo veía:
 * se llama {@code ...Port} y por eso escapaba en silencio.
 *
 * <p>
 * <b>Lo que decide este SQL.</b> Qué submódulos compró el cliente el día que
 * firmó. Si expande de menos, el cliente de un plan empaquetado se queda sin
 * permisos; si expande de más, se lleva gratis lo que no pagó; y si el
 * {@code company_id} no acota, una clínica arrastra aquí la composición de
 * otra.
 *
 * <p>
 * <b>El escenario está montado para que un SQL equivocado se vea.</b> Los ids
 * de submódulo (71xx), de artículo (713x) y de línea (711x) están en rangos
 * disjuntos, así que cruzar dos columnas no puede dar por casualidad el
 * resultado correcto; hay más de un submódulo por artículo, así que un
 * {@code WHERE} que se pierda sale a la luz; y hay una línea de otra empresa
 * con la misma forma, así que una cláusula de empresa que desaparezca se caza
 * en vez de acertar por coincidencia.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSubscriptionItemCompositionPort — la foto de la composición contra MySQL real")
class SubscriptionItemCompositionPortIT extends AbstractDataJpaTest {

    /** Línea propia a la que se le congela un MODULE suelto. */
    private static final Long LINEA_MODULO = 7110L;
    /** Línea propia a la que se le congela un BUNDLE, que hay que expandir. */
    private static final Long LINEA_PAQUETE = 7111L;
    /** Línea propia de capacidad: no ata ningún submódulo. Cero legítimo. */
    private static final Long LINEA_CAPACIDAD = 7112L;
    /** Línea de OTRA clínica, misma forma. La mitad del valor de esta rodaja. */
    private static final Long LINEA_AJENA = 7113L;

    /** Submódulo que el MODULE ata directo, y también un componente del BUNDLE. */
    private static final Long SM_A = 7120L;
    /** Segundo submódulo directo: sin él, un {@code WHERE} perdido no se vería. */
    private static final Long SM_B = 7121L;
    /** Solo se alcanza por enlaces deshabilitados. Nunca debe congelarse. */
    private static final Long SM_C = 7122L;
    /** Submódulo con {@code sub_modules.enabled = FALSE}. Tampoco. */
    private static final Long SM_APAGADO = 7123L;
    /**
     * <b>La pieza clave de la expansión.</b> Cuelga de los DOS componentes del
     * paquete, así que el {@code SELECT} lo propone dos veces y el
     * {@code ON DUPLICATE KEY UPDATE} tiene que dejar una sola fila.
     */
    private static final Long SM_COMPARTIDO = 7124L;
    /** Cuelga del paquete DIRECTAMENTE: prueba que la primera sentencia corre. */
    private static final Long SM_DEL_PAQUETE = 7125L;

    private static final Long ART_MODULO = 7130L;
    private static final Long ART_PAQUETE = 7131L;
    private static final Long ART_COMPONENTE_UNO = 7132L;
    private static final Long ART_COMPONENTE_DOS = 7133L;
    /** Componente cuyo enlace al paquete está deshabilitado. */
    private static final Long ART_COMPONENTE_APAGADO = 7134L;
    /** Artículo de capacidad: sin composición. */
    private static final Long ART_CAPACIDAD = 7135L;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaSubscriptionItemCompositionPort port;

    @BeforeEach
    void sembrarLaComposicion() {
        SchemaSeed.seed(entityManager);
        port = new JpaSubscriptionItemCompositionPort(entityManager);

        submodulo(SM_A, "TEST_COMP_SM_A", true);
        submodulo(SM_B, "TEST_COMP_SM_B", true);
        submodulo(SM_C, "TEST_COMP_SM_C", true);
        submodulo(SM_APAGADO, "TEST_COMP_SM_APAGADO", false);
        submodulo(SM_COMPARTIDO, "TEST_COMP_SM_COMPARTIDO", true);
        submodulo(SM_DEL_PAQUETE, "TEST_COMP_SM_DEL_PAQUETE", true);

        articulo(ART_MODULO, "TEST_COMP_MODULO", "Modulo suelto", "MODULE", null);
        articulo(ART_PAQUETE, "TEST_COMP_PAQUETE", "Paquete", "BUNDLE", null);
        articulo(ART_COMPONENTE_UNO, "TEST_COMP_COMPONENTE_1", "Componente uno", "MODULE", null);
        articulo(ART_COMPONENTE_DOS, "TEST_COMP_COMPONENTE_2", "Componente dos", "MODULE", null);
        articulo(ART_COMPONENTE_APAGADO, "TEST_COMP_COMPONENTE_3", "Componente apagado", "MODULE",
                null);
        articulo(ART_CAPACIDAD, "TEST_COMP_CAPACIDAD", "Usuario adicional", "CAPACITY", "USER");

        // El MODULE ata A y B; y ademas dos submodulos que NO deben salir, cada uno
        // por un motivo distinto: SM_APAGADO porque el submodulo esta apagado, SM_C
        // porque el enlace lo esta. Dos motivos distintos, dos filas distintas: con
        // una sola no se sabria cual de los dos predicados hizo el trabajo.
        ataSubmodulo(7140L, ART_MODULO, SM_A, true);
        ataSubmodulo(7141L, ART_MODULO, SM_B, true);
        ataSubmodulo(7142L, ART_MODULO, SM_APAGADO, true);
        ataSubmodulo(7143L, ART_MODULO, SM_C, false);

        ataSubmodulo(7144L, ART_PAQUETE, SM_DEL_PAQUETE, true);
        ataSubmodulo(7145L, ART_COMPONENTE_UNO, SM_A, true);
        ataSubmodulo(7146L, ART_COMPONENTE_UNO, SM_COMPARTIDO, true);
        ataSubmodulo(7147L, ART_COMPONENTE_DOS, SM_B, true);
        ataSubmodulo(7148L, ART_COMPONENTE_DOS, SM_COMPARTIDO, true);
        ataSubmodulo(7149L, ART_COMPONENTE_APAGADO, SM_C, true);

        componente(7150L, ART_PAQUETE, ART_COMPONENTE_UNO, true);
        componente(7151L, ART_PAQUETE, ART_COMPONENTE_DOS, true);
        componente(7152L, ART_PAQUETE, ART_COMPONENTE_APAGADO, false);

        linea(LINEA_MODULO, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, ART_MODULO,
                "TEST_COMP_MODULO", "MODULE", null);
        linea(LINEA_PAQUETE, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, ART_PAQUETE,
                "TEST_COMP_PAQUETE", "BUNDLE", null);
        linea(LINEA_CAPACIDAD, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, ART_CAPACIDAD,
                "TEST_COMP_CAPACIDAD", "CAPACITY", "USER");
        linea(LINEA_AJENA, SchemaSeed.OTRA_COMPANY_ID, SchemaSeed.OTRA_SUBSCRIPTION_ID, ART_PAQUETE,
                "TEST_COMP_PAQUETE", "BUNDLE", null);

        entityManager.flush();
    }

    @Nested
    @DisplayName("Congelado directo")
    class CongeladoDirecto {

        @Test
        @DisplayName("congela los submódulos que el artículo ata por sí mismo, y solo esos")
        void congela_los_submodulos_que_el_articulo_ata_por_si_mismo() {
            assertThat(port.freeze(SchemaSeed.COMPANY_ID, LINEA_MODULO, ART_MODULO)).isEqualTo(2);

            assertThat(port.findFrozenSubModuleIds(SchemaSeed.COMPANY_ID, LINEA_MODULO))
                    .containsExactly(SM_A, SM_B);
        }

        @Test
        @DisplayName("un submódulo deshabilitado no entra en la foto aunque el enlace exista")
        void un_submodulo_deshabilitado_no_entra_en_la_foto() {
            // Se comprueba primero que el ENLACE existe y esta habilitado. Sin esta
            // precondicion el test pasaria igual si el fixture no hubiera creado la fila,
            // que es la forma silenciosa de que una prueba deje de proteger: verde por
            // ausencia de dato, no porque el JOIN con sub_modules haga su trabajo.
            assertThat(enlacesHabilitados(ART_MODULO, SM_APAGADO)).isEqualTo(1);

            port.freeze(SchemaSeed.COMPANY_ID, LINEA_MODULO, ART_MODULO);

            assertThat(port.findFrozenSubModuleIds(SchemaSeed.COMPANY_ID, LINEA_MODULO))
                    .doesNotContain(SM_APAGADO);
        }

        @Test
        @DisplayName("un enlace deshabilitado del catálogo tampoco entra")
        void un_enlace_deshabilitado_del_catalogo_tampoco_entra() {
            assertThat(filasDeEnlace(ART_MODULO, SM_C)).isEqualTo(1);

            port.freeze(SchemaSeed.COMPANY_ID, LINEA_MODULO, ART_MODULO);

            assertThat(port.findFrozenSubModuleIds(SchemaSeed.COMPANY_ID, LINEA_MODULO))
                    .doesNotContain(SM_C);
        }

        @Test
        @DisplayName("una línea de capacidad congela cero submódulos, y eso no es un error")
        void una_linea_de_capacidad_congela_cero_submodulos() {
            assertThat(port.freeze(SchemaSeed.COMPANY_ID, LINEA_CAPACIDAD, ART_CAPACIDAD)).isZero();

            assertThat(port.findFrozenSubModuleIds(SchemaSeed.COMPANY_ID, LINEA_CAPACIDAD))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Expansión del paquete")
    class ExpansionDelPaquete {

        @Test
        @DisplayName("un paquete congela lo suyo y lo de sus componentes, en una sola pasada")
        void un_paquete_congela_lo_suyo_y_lo_de_sus_componentes() {
            // Cuatro y no cinco: SM_COMPARTIDO llega por los dos componentes y la foto es
            // un conjunto. Es el numero el que distingue «expande bien» de «expande dos
            // veces»; con `hasSizeGreaterThan` no se distinguirian.
            assertThat(port.freeze(SchemaSeed.COMPANY_ID, LINEA_PAQUETE, ART_PAQUETE)).isEqualTo(4);

            assertThat(port.findFrozenSubModuleIds(SchemaSeed.COMPANY_ID, LINEA_PAQUETE))
                    .containsExactly(SM_A, SM_B, SM_COMPARTIDO, SM_DEL_PAQUETE);
        }

        @Test
        @DisplayName("el submódulo al que llegan dos componentes se congela una sola vez")
        void el_submodulo_al_que_llegan_dos_componentes_se_congela_una_sola_vez() {
            // La precondicion es lo que da valor al caso: SM_COMPARTIDO tiene DOS caminos
            // vivos hasta el paquete. Sin comprobarlo, un fixture que perdiera uno dejaria
            // el test en verde sin haber probado nunca la deduplicacion.
            assertThat(caminosHastaElPaquete(SM_COMPARTIDO)).isEqualTo(2);

            port.freeze(SchemaSeed.COMPANY_ID, LINEA_PAQUETE, ART_PAQUETE);

            assertThat(filasCongeladas(LINEA_PAQUETE, SM_COMPARTIDO)).isEqualTo(1);
        }

        @Test
        @DisplayName("un componente cuyo enlace al paquete está deshabilitado no aporta nada")
        void un_componente_con_enlace_deshabilitado_no_aporta_nada() {
            assertThat(filasDeComponente(ART_PAQUETE, ART_COMPONENTE_APAGADO)).isEqualTo(1);

            port.freeze(SchemaSeed.COMPANY_ID, LINEA_PAQUETE, ART_PAQUETE);

            assertThat(port.findFrozenSubModuleIds(SchemaSeed.COMPANY_ID, LINEA_PAQUETE))
                    .doesNotContain(SM_C);
        }
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        /**
         * <b>El caso que justifica el {@code INSERT IGNORE}, y el que más fácil se
         * escapa.</b> {@code IGNORE} degrada a warning el error de clave duplicada: una
         * segunda pasada no revienta, pero tampoco avisa. La única forma de saber si
         * <em>ignoró</em> o si <em>duplicó</em> es contar filas, no confiar en que no
         * hubo excepción.
         */
        @Test
        @DisplayName("una segunda pasada no duplica la foto: ni una fila más")
        void una_segunda_pasada_no_duplica_la_foto() {
            int primera = port.freeze(SchemaSeed.COMPANY_ID, LINEA_PAQUETE, ART_PAQUETE);
            long trasLaPrimera = filasCongeladas(LINEA_PAQUETE);

            int segunda = port.freeze(SchemaSeed.COMPANY_ID, LINEA_PAQUETE, ART_PAQUETE);

            assertThat(primera).isEqualTo(4);
            assertThat(segunda).isZero();
            assertThat(filasCongeladas(LINEA_PAQUETE)).isEqualTo(trasLaPrimera).isEqualTo(4);
        }

        @Test
        @DisplayName("la segunda pasada devuelve la misma foto, no una acumulada")
        void la_segunda_pasada_devuelve_la_misma_foto() {
            port.freeze(SchemaSeed.COMPANY_ID, LINEA_MODULO, ART_MODULO);
            port.freeze(SchemaSeed.COMPANY_ID, LINEA_MODULO, ART_MODULO);

            assertThat(port.findFrozenSubModuleIds(SchemaSeed.COMPANY_ID, LINEA_MODULO))
                    .containsExactly(SM_A, SM_B);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * <b>Lo que este caso deja escrito, y no es lo que parece.</b> El
         * {@code company_id} viaja como parámetro, así que el {@code SELECT} de origen
         * —que solo mira el catálogo, global— propone las filas igual. Quien rechaza la
         * escritura es la clave foránea compuesta
         * {@code (company_id, subscription_item_id) → subscription_items(company_id,
         * id)}.
         *
         * <p>
         * <b>Y ahora esa violación SE OYE.</b> Con la sentencia anterior
         * —{@code INSERT IGNORE}— la foránea se degradaba a un aviso: sin excepción,
         * sin log, devolviendo cero, indistinguible del cero legítimo de una línea de
         * capacidad sin composición. Los dos llamantes descartan el retorno, así que
         * una firma cruzada entre clínicas quedaba sin foto —sin permisos— y nadie se
         * enteraba. Con {@code ON DUPLICATE KEY UPDATE} la idempotencia se conserva
         * (ver {@code Idempotencia}) y la foránea vuelve a lanzar.
         */
        /**
         * <b>Sin aserciones después del lanzamiento, y no por pereza.</b> Una violación
         * de restricción marca la sesión de Hibernate para rollback: cualquier consulta
         * posterior en la misma transacción falla por eso y no por lo que se quisiera
         * comprobar. Que no quedó ni una fila lo garantiza el propio rollback del
         * {@code @DataJpaTest}, y que la foto ajena no se lee lo cubre el caso
         * siguiente.
         */
        @Test
        @DisplayName("firmar con la empresa equivocada lanza en vez de devolver cero en silencio")
        void firmar_con_la_empresa_equivocada_lanza() {
            assertThatThrownBy(
                    () -> port.freeze(SchemaSeed.OTRA_COMPANY_ID, LINEA_PAQUETE, ART_PAQUETE))
                    .isInstanceOf(PersistenceException.class);
        }

        @Test
        @DisplayName("la foto de una clínica no se le lee a la otra")
        void la_foto_de_una_clinica_no_se_le_lee_a_la_otra() {
            port.freeze(SchemaSeed.COMPANY_ID, LINEA_MODULO, ART_MODULO);
            port.freeze(SchemaSeed.OTRA_COMPANY_ID, LINEA_AJENA, ART_PAQUETE);

            // Las dos direcciones. Con una sola, un `company_id` que desapareciera del
            // WHERE se cazaria por un lado y no por el otro segun que fila llegue antes.
            assertThat(port.findFrozenSubModuleIds(SchemaSeed.COMPANY_ID, LINEA_MODULO))
                    .containsExactly(SM_A, SM_B);
            assertThat(port.findFrozenSubModuleIds(SchemaSeed.OTRA_COMPANY_ID, LINEA_AJENA))
                    .containsExactly(SM_A, SM_B, SM_COMPARTIDO, SM_DEL_PAQUETE);
            assertThat(port.findFrozenSubModuleIds(SchemaSeed.OTRA_COMPANY_ID, LINEA_MODULO))
                    .isEmpty();
            assertThat(port.findFrozenSubModuleIds(SchemaSeed.COMPANY_ID, LINEA_AJENA)).isEmpty();
        }

        @Test
        @DisplayName("una fila de composición dada de baja deja de leerse")
        void una_fila_de_composicion_dada_de_baja_deja_de_leerse() {
            port.freeze(SchemaSeed.COMPANY_ID, LINEA_MODULO, ART_MODULO);
            deshabilitaComposicion(LINEA_MODULO, SM_A);

            assertThat(port.findFrozenSubModuleIds(SchemaSeed.COMPANY_ID, LINEA_MODULO))
                    .containsExactly(SM_B);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("sin empresa no se congela nada: falla antes de tocar la base")
        void sin_empresa_no_se_congela_nada() {
            assertThatThrownBy(() -> port.freeze(null, LINEA_MODULO, ART_MODULO))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("required");
        }

        @Test
        @DisplayName("sin línea no se congela nada")
        void sin_linea_no_se_congela_nada() {
            assertThatThrownBy(() -> port.freeze(SchemaSeed.COMPANY_ID, null, ART_MODULO))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("required");
        }

        @Test
        @DisplayName("sin artículo no se congela nada")
        void sin_articulo_no_se_congela_nada() {
            assertThatThrownBy(() -> port.freeze(SchemaSeed.COMPANY_ID, LINEA_MODULO, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("required");
        }
    }

    private long filasCongeladas(Long subscriptionItemId) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM subscription_item_sub_modules
                 WHERE subscription_item_id = :itemId
                """).setParameter("itemId", subscriptionItemId).getSingleResult()).longValue();
    }

    private long filasCongeladas(Long subscriptionItemId, Long subModuleId) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM subscription_item_sub_modules
                 WHERE subscription_item_id = :itemId AND sub_module_id = :subModuleId
                """).setParameter("itemId", subscriptionItemId)
                .setParameter("subModuleId", subModuleId).getSingleResult()).longValue();
    }

    private long filasDeEnlace(Long catalogItemId, Long subModuleId) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM catalog_item_sub_modules
                 WHERE catalog_item_id = :articulo AND sub_module_id = :subModuleId
                """).setParameter("articulo", catalogItemId)
                .setParameter("subModuleId", subModuleId).getSingleResult()).longValue();
    }

    private long enlacesHabilitados(Long catalogItemId, Long subModuleId) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM catalog_item_sub_modules
                 WHERE catalog_item_id = :articulo AND sub_module_id = :subModuleId
                   AND enabled = TRUE
                """).setParameter("articulo", catalogItemId)
                .setParameter("subModuleId", subModuleId).getSingleResult()).longValue();
    }

    private long filasDeComponente(Long bundleId, Long componentId) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM bundle_components
                 WHERE bundle_item_id = :paquete AND component_item_id = :componente
                """).setParameter("paquete", bundleId).setParameter("componente", componentId)
                .getSingleResult()).longValue();
    }

    /** Cuántos componentes vivos del paquete llegan a ese submódulo. */
    private long caminosHastaElPaquete(Long subModuleId) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                  FROM bundle_components bc
                  JOIN catalog_item_sub_modules cism
                       ON cism.catalog_item_id = bc.component_item_id AND cism.enabled = TRUE
                 WHERE bc.bundle_item_id = :paquete
                   AND bc.enabled = TRUE
                   AND cism.sub_module_id = :subModuleId
                """).setParameter("paquete", ART_PAQUETE).setParameter("subModuleId", subModuleId)
                .getSingleResult()).longValue();
    }

    private void deshabilitaComposicion(Long subscriptionItemId, Long subModuleId) {
        entityManager.createNativeQuery("""
                UPDATE subscription_item_sub_modules SET enabled = FALSE
                 WHERE subscription_item_id = :itemId AND sub_module_id = :subModuleId
                """).setParameter("itemId", subscriptionItemId)
                .setParameter("subModuleId", subModuleId).executeUpdate();
    }

    private void submodulo(Long id, String code, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO sub_modules (id, name, code, module_id, created_date, enabled, version,
                                         is_sellable, read_only_capable)
                VALUES (:id, :name, :code, :moduleId, '2026-01-01 00:00:00', :enabled, 0,
                        true, true)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("name", "Submodulo " + code)
                .setParameter("code", code).setParameter("moduleId", SchemaSeed.MODULE_ID)
                .setParameter("enabled", enabled).executeUpdate();
    }

    /**
     * {@code capacity_unit} tiene FK contra {@code limit_dimensions(code)}: solo
     * ejes reales. {@code USER} es el que usa el resto de la suite.
     */
    private void articulo(Long id, String code, String name, String itemType, String capacityUnit) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_items (id, code, name, item_type, capacity_unit, is_core,
                                           min_quantity, max_quantity, sort_order, status,
                                           trial_eligibility, default_trial_days, trial_outcome,
                                           service_nature, created_date, enabled, version)
                VALUES (:id, :code, :name, :itemType, :capacityUnit, false, 1, NULL, 0, 'ACTIVE',
                        'NEVER_FREE', NULL, NULL, 'SOFTWARE_LICENSING',
                        '2026-01-01 00:00:00', true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .setParameter("itemType", itemType).setParameter("capacityUnit", capacityUnit)
                .executeUpdate();
    }

    private void ataSubmodulo(Long id, Long catalogItemId, Long subModuleId, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_item_sub_modules (id, catalog_item_id, sub_module_id,
                                                      created_date, enabled)
                VALUES (:id, :articulo, :subModuleId, '2026-01-01 00:00:00', :enabled)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("articulo", catalogItemId)
                .setParameter("subModuleId", subModuleId).setParameter("enabled", enabled)
                .executeUpdate();
    }

    private void componente(Long id, Long bundleId, Long componentId, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO bundle_components (id, bundle_item_id, component_item_id, quantity,
                                               created_date, enabled)
                VALUES (:id, :paquete, :componente, 1, '2026-01-01 00:00:00', :enabled)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("paquete", bundleId)
                .setParameter("componente", componentId).setParameter("enabled", enabled)
                .executeUpdate();
    }

    /**
     * Línea abierta ({@code effective_to} nulo). Cada una con su propio artículo:
     * {@code uq_subscription_items_current} es única sobre
     * {@code (subscription_id, current_item_marker, current_tier_marker)} y dos
     * líneas abiertas del mismo artículo en el mismo contrato chocarían.
     */
    private void linea(Long id, Long companyId, Long subscriptionId, Long catalogItemId,
            String itemCode, String itemType, String capacityUnit) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_items (id, company_id, subscription_id, catalog_item_id,
                                                item_code, item_name, item_type, capacity_unit,
                                                included_quantity, tax_treatment, quantity,
                                                unit_amount, tax_rate, tier_min, tier_max,
                                                months_in_cycle, charge_mode, trial_eligibility,
                                                max_trial_days, trial_end_date, activation_path,
                                                billing_effect, effective_from,
                                                effective_to, origin, succeeds_item_id,
                                                created_amendment_id,
                                                ended_amendment_id, created_date, enabled, version)
                VALUES (:id, :companyId, :subscriptionId, :articulo, :itemCode, :itemName,
                        :itemType, :capacityUnit, 0, 'TAXED', 1, 50000.00, 19.00, 1, NULL,
                        1, 'PAID', 'NEVER_FREE', 0, NULL, 'PLATFORM',
                        'NONE', '2026-01-01', NULL, 'INITIAL', NULL, NULL, NULL,
                        '2026-01-01 00:00:00', true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("companyId", companyId)
                .setParameter("subscriptionId", subscriptionId)
                .setParameter("articulo", catalogItemId).setParameter("itemCode", itemCode)
                .setParameter("itemName", "Linea " + itemCode).setParameter("itemType", itemType)
                .setParameter("capacityUnit", capacityUnit).executeUpdate();
    }
}
