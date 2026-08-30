package com.vetsoftware.app.pricelist.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.pricelist.domain.CatalogItemRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * El SQL nativo del slice {@code pricelist} contra {@code catalog_items}, con
 * MySQL real.
 *
 * <p>
 * <b>Por qué esta rodaja hacía falta, y por qué no la cubría la que ya
 * existe.</b> Hay <b>dos clases distintas llamadas
 * {@code JpaCatalogItemQueryPort}</b>: la anidada en
 * {@code quote.JpaCatalogQueryPorts}, que sí tiene rodaja
 * ({@code QuoteCatalogQueryPortsIT}), y esta, la de {@code pricelist}, que no
 * tenía ninguna. Buscar el nombre simple en {@code src/test} devuelve
 * resultados y hace pensar que está cubierta: no lo estaba. Y como se llama
 * {@code ...Port} y no {@code Jpa<Algo>Repository},
 * {@code ADAPTADOR_JPA_CON_RODAJA} tampoco la alcanza.
 *
 * <p>
 * <b>Lo que decide.</b> {@code findAllActiveIds()} es la puerta de la
 * publicación de una tarifa: si devuelve de más, la publicación se bloquea
 * exigiendo precio a un artículo que nadie puede comprar; si devuelve de menos,
 * se publica una tarifa con un artículo vendible sin precio.
 * {@code findAllByIds} es lo que evita el N+1 de la incidencia #379.
 *
 * <p>
 * <b>Cómo está montado para que un SQL equivocado se vea.</b> Los cinco
 * artículos del escenario <b>solo se diferencian en lo que la regla mira</b>
 * —mismo tipo, mismo prefijo de código— y cada motivo de exclusión tiene su
 * propia fila: uno en {@code DRAFT}, otro {@code DEPRECATED} y otro
 * {@code ACTIVE} pero de baja lógica. Con una sola fila excluida no se sabría
 * cuál de los dos predicados hizo el trabajo. Hay además <b>dos</b> artículos
 * que sí pasan, para que un {@code WHERE} perdido o un {@code LIMIT} implícito
 * salgan a la luz.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCatalogItemQueryPort (pricelist) — el catálogo que abre la publicación")
class PriceListCatalogItemQueryPortIT extends AbstractDataJpaTest {

    private static final Long ART_ACTIVO = 7410L;
    private static final Long ART_BORRADOR = 7411L;
    private static final Long ART_RETIRADO = 7412L;
    /** {@code ACTIVE} y aun así fuera: la baja lógica lo saca igual. */
    private static final Long ART_DE_BAJA = 7413L;
    /** Segundo activo: sin él, un {@code WHERE} perdido podría no verse. */
    private static final Long ART_ACTIVO_DOS = 7414L;

    private static final String CODIGO_ACTIVO = "TEST_PL_ACTIVO";
    private static final String NOMBRE_ACTIVO = "Articulo publicable";
    private static final String CODIGO_BORRADOR = "TEST_PL_BORRADOR";
    private static final String NOMBRE_BORRADOR = "Articulo en redaccion";

    @PersistenceContext
    private EntityManager entityManager;

    private JpaCatalogItemQueryPort port;

    @BeforeEach
    void sembrarElCatalogo() {
        SchemaSeed.seed(entityManager);
        port = new JpaCatalogItemQueryPort(entityManager);

        articulo(ART_ACTIVO, CODIGO_ACTIVO, NOMBRE_ACTIVO, "ACTIVE", true);
        articulo(ART_BORRADOR, CODIGO_BORRADOR, NOMBRE_BORRADOR, "DRAFT", true);
        articulo(ART_RETIRADO, "TEST_PL_RETIRADO", "Articulo retirado", "DEPRECATED", true);
        articulo(ART_DE_BAJA, "TEST_PL_DE_BAJA", "Articulo de baja", "ACTIVE", false);
        articulo(ART_ACTIVO_DOS, "TEST_PL_ACTIVO_DOS", "Segundo publicable", "ACTIVE", true);

        entityManager.flush();
    }

    @Nested
    @DisplayName("findAllActiveIds — la puerta de la publicación")
    class Publicables {

        @Test
        @DisplayName("solo los ACTIVE y habilitados abren la publicación")
        void solo_los_active_y_habilitados_abren_la_publicacion() {
            // Las cinco filas EXISTEN. Sin esta precondicion, un fixture que perdiera
            // cualquiera de los excluidos dejaria el caso en verde por ausencia de dato en
            // vez de por el filtro, que es la forma silenciosa de dejar de proteger.
            assertThat(filasConId(ART_BORRADOR)).isEqualTo(1);
            assertThat(filasConId(ART_RETIRADO)).isEqualTo(1);
            assertThat(filasConId(ART_DE_BAJA)).isEqualTo(1);

            assertThat(port.findAllActiveIds()).contains(ART_ACTIVO, ART_ACTIVO_DOS)
                    .doesNotContain(ART_BORRADOR, ART_RETIRADO, ART_DE_BAJA);
        }

        @Test
        @DisplayName("llegan ordenados por id ascendente")
        void llegan_ordenados_por_id_ascendente() {
            assertThat(port.findAllActiveIds()).isSorted();
        }

        @Test
        @DisplayName("no devuelve el mismo artículo dos veces")
        void no_devuelve_el_mismo_articulo_dos_veces() {
            assertThat(port.findAllActiveIds()).doesNotHaveDuplicates();
        }
    }

    @Nested
    @DisplayName("findAllByIds — el nombre de la fila que ya existe")
    class Resolucion {

        @Test
        @DisplayName("cada columna del SELECT cae en el campo que dice: id, código y nombre")
        void cada_columna_cae_en_el_campo_que_dice() {
            // El mapeo es POSICIONAL: `columns[1]` y `columns[2]` son los dos String de la
            // fila. Codigo y nombre son deliberadamente distintos, asi que si se cruzan la
            // consola pintaria "TEST_PL_ACTIVO" como nombre de producto y este caso lo
            // caza.
            assertThat(port.findAllByIds(List.of(ART_ACTIVO))).containsExactly(java.util.Map.entry(
                    ART_ACTIVO, new CatalogItemRef(ART_ACTIVO, CODIGO_ACTIVO, NOMBRE_ACTIVO)));
        }

        @Test
        @DisplayName("pedir uno devuelve uno: no arrastra el catálogo entero")
        void pedir_uno_devuelve_uno() {
            // Si el `WHERE id IN (:ids)` desapareciera, el mapa traeria todo el catalogo
            // -que en esta base son cientos de filas del changeset 308- y el resultado
            // seguiria conteniendo el articulo pedido. El tamaño es lo que lo distingue.
            assertThat(port.findAllByIds(List.of(ART_ACTIVO))).hasSize(1);
        }

        @Test
        @DisplayName("resuelve varios de una sola consulta, que es lo que evita el N+1")
        void resuelve_varios_de_una_sola_consulta() {
            assertThat(port.findAllByIds(List.of(ART_ACTIVO, ART_ACTIVO_DOS)))
                    .containsOnlyKeys(ART_ACTIVO, ART_ACTIVO_DOS);
        }

        /**
         * A diferencia de {@code quote}, que congela el artículo en un documento con
         * valor legal y por eso exige {@code ACTIVE}, aquí solo se pinta el nombre de
         * una fila que ya existe: esconder el de un {@code DRAFT} o un
         * {@code DEPRECATED} dejaría la tarifa histórica ilegible sin impedir nada. El
         * caso está en positivo para que la ausencia de filtro sea una decisión escrita
         * y no un descuido que nadie note.
         */
        @Test
        @DisplayName("resuelve también un artículo en DRAFT: aquí el estado no filtra")
        void resuelve_tambien_un_articulo_en_draft() {
            assertThat(port.findAllByIds(List.of(ART_BORRADOR)))
                    .containsExactly(java.util.Map.entry(ART_BORRADOR,
                            new CatalogItemRef(ART_BORRADOR, CODIGO_BORRADOR, NOMBRE_BORRADOR)));
        }

        @Test
        @DisplayName("un artículo de baja lógica no se resuelve")
        void un_articulo_de_baja_logica_no_se_resuelve() {
            assertThat(filasConId(ART_DE_BAJA)).isEqualTo(1);

            assertThat(port.findAllByIds(List.of(ART_DE_BAJA))).isEmpty();
        }

        @Test
        @DisplayName("los ids repetidos y los nulos no rompen la consulta")
        void los_ids_repetidos_y_los_nulos_no_rompen_la_consulta() {
            assertThat(port.findAllByIds(Arrays.asList(ART_ACTIVO, ART_ACTIVO, null)))
                    .containsOnlyKeys(ART_ACTIVO);
        }

        @Test
        @DisplayName("una colección vacía, nula o solo de nulos no llega a la base")
        void una_coleccion_vacia_nula_o_solo_de_nulos_no_llega_a_la_base() {
            assertThat(port.findAllByIds(List.of())).isEmpty();
            assertThat(port.findAllByIds(null)).isEmpty();
            assertThat(port.findAllByIds(Arrays.asList((Long) null, null))).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class Individual {

        @Test
        @DisplayName("resuelve el artículo pedido")
        void resuelve_el_articulo_pedido() {
            assertThat(port.findById(ART_ACTIVO))
                    .contains(new CatalogItemRef(ART_ACTIVO, CODIGO_ACTIVO, NOMBRE_ACTIVO));
        }

        @Test
        @DisplayName("un artículo que no existe, o un id nulo, devuelven vacío")
        void un_articulo_que_no_existe_o_un_id_nulo_devuelven_vacio() {
            assertThat(port.findById(7499L)).isEmpty();
            assertThat(port.findById(null)).isEmpty();
        }
    }

    private long filasConId(Long id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM catalog_items WHERE id = :id")
                .setParameter("id", id).getSingleResult()).longValue();
    }

    /**
     * {@code uq_catalog_items_code} es único global y el contenedor de
     * Testcontainers lo comparte toda la suite: de ahí el prefijo {@code TEST_PL_}
     * y el rango de ids 74xx, disjuntos de los que usan las demás rodajas. Es la
     * incidencia #647, que se cobró ya un fixture de esta casa.
     */
    private void articulo(Long id, String code, String name, String status, boolean enabled) {
        entityManager.createNativeQuery("""
                INSERT INTO catalog_items (id, code, name, item_type, capacity_unit, is_core,
                                           min_quantity, max_quantity, sort_order, status,
                                           trial_eligibility, default_trial_days, trial_outcome,
                                           service_nature, created_date, enabled, version)
                VALUES (:id, :code, :name, 'MODULE', NULL, false, 1, NULL, 0, :status,
                        'NEVER_FREE', NULL, NULL, 'SOFTWARE_LICENSING',
                        '2026-01-01 00:00:00', :enabled, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", id).setParameter("code", code).setParameter("name", name)
                .setParameter("status", status).setParameter("enabled", enabled).executeUpdate();
    }
}
