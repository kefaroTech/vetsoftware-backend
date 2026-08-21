package com.vetsoftware.app.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.inventory.application.command.SearchCountsQuery;
import com.vetsoftware.app.inventory.application.dto.InventoryCountLineView;
import com.vetsoftware.app.inventory.application.dto.InventoryCountView;
import com.vetsoftware.app.inventory.domain.InventoryCount;
import com.vetsoftware.app.inventory.domain.InventoryCountLine;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de los conteos fisicos contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve.</b> Este adaptador guarda la misma informacion
 * dos veces a proposito y las dos copias se leen por caminos distintos:
 *
 * <ul>
 * <li>{@code total_lines} y {@code adjusted_lines} estan <b>desnormalizados</b>
 * en la cabecera para que el listado no cargue las lineas. El detalle, en
 * cambio, los recalcula desde el agregado. Si {@code save} dejara de escribir
 * esas dos columnas, el listado mostraria ceros y el detalle los numeros
 * correctos: una divergencia que solo aparece cuando hay dos consultas reales
 * contra la misma fila.</li>
 * <li>{@code difference} se persiste por linea «para reporte SQL». Ningun
 * camino Java la lee —el dominio la recalcula—, asi que un test en memoria no
 * puede notar que la columna se quedo en cero. Aqui se comprueba leyendola con
 * SQL nativo, que es como la leen los reportes.</li>
 * <li>La entidad lleva {@code @SQLRestriction("enabled = true")}. Ese filtro lo
 * inyecta Hibernate en el SQL, no el codigo del adaptador: sin base real no hay
 * nada que ejercitarlo.</li>
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaInventoryCountRepository — los conteos fisicos contra MySQL real")
class InventoryCountPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;
    private static final Long PRODUCT = SchemaSeed.PRODUCT_ID;
    private static final Long OTRO_PRODUCT = SchemaSeed.OTRO_PRODUCT_ID;
    private static final Long EMPLOYEE = SchemaSeed.EMPLOYEE_ID;

    private static final LocalDateTime ENERO_10 = LocalDateTime.of(2026, 1, 10, 9, 0);
    private static final LocalDateTime ENERO_20 = LocalDateTime.of(2026, 1, 20, 9, 0);
    private static final LocalDateTime FEBRERO_5 = LocalDateTime.of(2026, 2, 5, 9, 0);

    @Autowired
    private JpaInventoryCountRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    /** Conteo con una linea que cuadra y otra que no: 2 lineas, 1 ajustada. */
    private static List<InventoryCountLine> unaCuadraYOtraNo() {
        return List.of(InventoryCountLine.create(PRODUCT, 10, 7),
                InventoryCountLine.create(OTRO_PRODUCT, 4, 4));
    }

    private InventoryCount conteo(Long companyId, Long branchId, LocalDateTime cuando) {
        return conteo(companyId, branchId, cuando, true, unaCuadraYOtraNo());
    }

    private InventoryCount conteo(Long companyId, Long branchId, LocalDateTime cuando,
            boolean habilitado, List<InventoryCountLine> lineas) {
        return repository.save(new InventoryCount(null, companyId, branchId, "conteo mensual",
                EMPLOYEE, cuando, habilitado, lineas));
    }

    /**
     * Fuerza el INSERT y vacia el contexto: sin el {@code clear()} las consultas
     * devolverian las instancias que ya estan en memoria y ni el
     * {@code @SQLRestriction} ni las columnas desnormalizadas se ejercitarian.
     */
    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Lee la columna como la leen los reportes: por SQL, sin pasar por el dominio.
     */
    private int diferenciaPersistida(Long countId, Long productId) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT difference FROM inventory_count_line
                WHERE count_id = ? AND product_id = ?
                """).setParameter(1, countId).setParameter(2, productId).getSingleResult())
                .intValue();
    }

    private PageResult<InventoryCountView> listar(Long branchId, int page, int pageSize) {
        return repository.search(new SearchCountsQuery(COMPANY, branchId, page, pageSize));
    }

    @Nested
    @DisplayName("save — la sesion y sus lineas")
    class Guardado {

        @Test
        @DisplayName("persiste la sesion con sus lineas y devuelve el id asignado")
        void persiste_la_sesion_con_sus_lineas() {
            InventoryCount guardado = conteo(COMPANY, BRANCH, ENERO_10);
            releerDesdeLaBase();

            InventoryCountView detalle = repository.findDetail(COMPANY, guardado.getId())
                    .orElseThrow();

            assertThat(guardado.getId()).isNotNull();
            assertThat(detalle.branchId()).isEqualTo(BRANCH);
            assertThat(detalle.note()).isEqualTo("conteo mensual");
            assertThat(detalle.countedBy()).isEqualTo(EMPLOYEE);
            assertThat(detalle.createdDate()).isEqualTo(ENERO_10);
            assertThat(detalle.lines()).extracting(InventoryCountLineView::productId)
                    .containsExactlyInAnyOrder(PRODUCT, OTRO_PRODUCT);
        }

        @Test
        @DisplayName("cada linea conserva lo que habia en el sistema y lo que se conto")
        void cada_linea_conserva_sistema_y_contado() {
            InventoryCount guardado = conteo(COMPANY, BRANCH, ENERO_10);
            releerDesdeLaBase();

            assertThat(repository.findDetail(COMPANY, guardado.getId()).orElseThrow().lines())
                    .filteredOn(l -> l.productId().equals(PRODUCT)).singleElement()
                    .satisfies(linea -> {
                        assertThat(linea.systemQuantity()).isEqualTo(10);
                        assertThat(linea.countedQuantity()).isEqualTo(7);
                        assertThat(linea.difference()).isEqualTo(-3);
                    });
        }

        @Test
        @DisplayName("la diferencia con signo queda escrita en la columna que leen los reportes")
        void la_diferencia_queda_escrita_en_la_columna() {
            // El dominio recalcula la diferencia, asi que si save dejara de escribir esta
            // columna ningun camino Java lo notaria: solo los reportes SQL, ya en
            // produccion, verian ceros donde hay faltantes.
            InventoryCount guardado = conteo(COMPANY, BRANCH, ENERO_10);
            releerDesdeLaBase();

            assertThat(diferenciaPersistida(guardado.getId(), PRODUCT)).isEqualTo(-3);
            assertThat(diferenciaPersistida(guardado.getId(), OTRO_PRODUCT)).isZero();
        }

        @Test
        @DisplayName("los contadores desnormalizados de la cabecera cuadran con las lineas")
        void los_contadores_desnormalizados_cuadran() {
            // El listado NO carga las lineas: lee estas dos columnas. El detalle las
            // recalcula. Cuando las dos copias se separan, la pantalla de historial miente
            // sobre cuantos ajustes genero cada conteo.
            conteo(COMPANY, BRANCH, ENERO_10);
            releerDesdeLaBase();

            assertThat(listar(BRANCH, 0, 20).content()).singleElement().satisfies(resumen -> {
                assertThat(resumen.totalLines()).isEqualTo(2);
                assertThat(resumen.adjustedLines()).isEqualTo(1);
            });
        }
    }

    @Nested
    @DisplayName("findDetail — el detalle y a quien pertenece")
    class Detalle {

        @Test
        @DisplayName("el detalle de otra empresa no se entrega")
        void el_detalle_de_otra_empresa_no_se_entrega() {
            // El id de un conteo es global: sin el filtro por empresa, adivinar un numero
            // bastaria para leer el inventario contado de otro tenant.
            InventoryCount ajeno = conteo(OTRA_COMPANY, BRANCH, ENERO_10);
            releerDesdeLaBase();

            assertThat(repository.findDetail(COMPANY, ajeno.getId())).isEmpty();
        }

        @Test
        @DisplayName("un conteo inexistente devuelve vacio, no un fallo")
        void un_conteo_inexistente_devuelve_vacio() {
            assertThat(repository.findDetail(COMPANY, 999_999L)).isEmpty();
        }

        @Test
        @DisplayName("las lineas vienen hidratadas aunque el contexto este vacio")
        void las_lineas_vienen_hidratadas() {
            // El @EntityGraph es lo que las trae en la misma consulta. Sin el, leer
            // detalle.lines() fuera de la sesion original seria una LazyInitialization o,
            // peor, una consulta por conteo en el historial.
            InventoryCount guardado = conteo(COMPANY, BRANCH, ENERO_10);
            releerDesdeLaBase();

            assertThat(repository.findDetail(COMPANY, guardado.getId()).orElseThrow().lines())
                    .hasSize(2);
        }
    }

    @Nested
    @DisplayName("search — el historial de conteos")
    class Historial {

        @Test
        @DisplayName("el resumen no carga las lineas pero si los contadores")
        void el_resumen_no_carga_las_lineas() {
            conteo(COMPANY, BRANCH, ENERO_10);
            releerDesdeLaBase();

            assertThat(listar(BRANCH, 0, 20).content()).singleElement().satisfies(resumen -> {
                assertThat(resumen.lines()).isEmpty();
                assertThat(resumen.totalLines()).isEqualTo(2);
            });
        }

        @Test
        @DisplayName("el mas reciente va primero")
        void el_mas_reciente_va_primero() {
            InventoryCount enero = conteo(COMPANY, BRANCH, ENERO_10);
            InventoryCount febrero = conteo(COMPANY, BRANCH, FEBRERO_5);
            releerDesdeLaBase();

            assertThat(listar(BRANCH, 0, 20).content()).extracting(InventoryCountView::id)
                    .containsExactly(febrero.getId(), enero.getId());
        }

        @Test
        @DisplayName("una sede null trae los conteos de todas las sedes")
        void una_sede_null_trae_todas_las_sedes() {
            conteo(COMPANY, BRANCH, ENERO_10);
            conteo(COMPANY, OTRA_BRANCH, ENERO_20);
            releerDesdeLaBase();

            assertThat(listar(null, 0, 20).content()).extracting(InventoryCountView::branchId)
                    .containsExactlyInAnyOrder(BRANCH, OTRA_BRANCH);
        }

        @Test
        @DisplayName("con sede solo trae la de esa sede")
        void con_sede_solo_trae_la_de_esa_sede() {
            conteo(COMPANY, BRANCH, ENERO_10);
            conteo(COMPANY, OTRA_BRANCH, ENERO_20);
            releerDesdeLaBase();

            assertThat(listar(OTRA_BRANCH, 0, 20).content())
                    .extracting(InventoryCountView::branchId).containsExactly(OTRA_BRANCH);
        }

        @Test
        @DisplayName("el historial de otra empresa no se mezcla")
        void el_historial_de_otra_empresa_no_se_mezcla() {
            InventoryCount propio = conteo(COMPANY, BRANCH, ENERO_10);
            conteo(OTRA_COMPANY, BRANCH, ENERO_20);
            releerDesdeLaBase();

            assertThat(listar(null, 0, 20).content()).extracting(InventoryCountView::id)
                    .containsExactly(propio.getId());
        }

        @Test
        @DisplayName("las dos paginas no repiten ni se saltan un conteo")
        void las_dos_paginas_no_repiten_ni_se_saltan() {
            InventoryCount enero10 = conteo(COMPANY, BRANCH, ENERO_10);
            InventoryCount enero20 = conteo(COMPANY, BRANCH, ENERO_20);
            InventoryCount febrero5 = conteo(COMPANY, BRANCH, FEBRERO_5);
            releerDesdeLaBase();

            PageResult<InventoryCountView> primera = listar(BRANCH, 0, 2);
            PageResult<InventoryCountView> segunda = listar(BRANCH, 1, 2);

            assertThat(primera.totalElements()).isEqualTo(3L);
            assertThat(primera.totalPages()).isEqualTo(2);
            assertThat(primera.content()).extracting(InventoryCountView::id)
                    .containsExactly(febrero5.getId(), enero20.getId());
            assertThat(segunda.content()).extracting(InventoryCountView::id)
                    .containsExactly(enero10.getId());
        }

        @Test
        @DisplayName("un conteo anulado desaparece del historial y del detalle")
        void un_conteo_anulado_desaparece() {
            // El @SQLRestriction de la entidad es un filtro que anade Hibernate al SQL. Si
            // se cayera, un conteo anulado volveria a contar en el historial de auditoria
            // y sus ajustes se leerian como vigentes.
            InventoryCount anulado = conteo(COMPANY, BRANCH, ENERO_10, false, unaCuadraYOtraNo());
            releerDesdeLaBase();

            assertThat(listar(BRANCH, 0, 20).content()).isEmpty();
            assertThat(repository.findDetail(COMPANY, anulado.getId())).isEmpty();
        }

        @Test
        @DisplayName("sin conteos devuelve una pagina vacia con sus metadatos, no null")
        void sin_conteos_devuelve_pagina_vacia() {
            PageResult<InventoryCountView> pagina = listar(BRANCH, 0, 20);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isZero();
            assertThat(pagina.totalPages()).isZero();
        }
    }
}
