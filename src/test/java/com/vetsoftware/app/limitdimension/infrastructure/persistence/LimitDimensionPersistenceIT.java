package com.vetsoftware.app.limitdimension.infrastructure.persistence;

import static com.vetsoftware.app.testsupport.EngineConstraint.assertViolates;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.limitdimension.domain.LimitDimension;
import com.vetsoftware.app.limitdimension.domain.MeasureKind;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Los ocho ejes llegan poblados por el changeset 313, así que esta rodaja lee
 * lo que la migración sembró en vez de inventarse su propio catálogo: si el
 * seed y el test declararan cada uno su {@code ANIMAL}, el segundo chocaría
 * contra {@code uq_limit_dimensions_code} y la prueba estaría midiendo una fila
 * que no es la que corre en producción.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaLimitDimensionRepository — el catálogo de ejes contra MySQL real")
class LimitDimensionPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaLimitDimensionRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("lee el eje de mascotas que sembró la migración, acumulativo y con sus treinta"
            + " días de enfriamiento")
    void lee_el_eje_de_mascotas_que_sembro_la_migracion() {
        assertThat(repository.findByCode("ANIMAL")).get().satisfies(eje -> {
            assertThat(eje.getMeasureKind()).isEqualTo(MeasureKind.CUMULATIVE);
            assertThat(eje.getReleaseDelayDays()).isEqualTo(30);
            assertThat(eje.getSubModule()).isNotNull();
        });
    }

    @Test
    @DisplayName("los ejes de flujo y de stock llegan sin enfriamiento, que es lo que la"
            + " restricción del motor exige")
    void los_ejes_de_flujo_y_de_stock_llegan_sin_enfriamiento() {
        assertThat(repository.findByCode("APPOINTMENT")).get().satisfies(eje -> {
            assertThat(eje.getMeasureKind()).isEqualTo(MeasureKind.FLOW);
            assertThat(eje.getReleaseDelayDays()).isNull();
        });
        assertThat(repository.findByCode("USER")).get().satisfies(eje -> {
            assertThat(eje.getMeasureKind()).isEqualTo(MeasureKind.STOCK);
            assertThat(eje.getReleaseDelayDays()).isNull();
        });
    }

    @Test
    @DisplayName("guarda un eje nuevo y lo devuelve con su id: vender un límite es insertar una"
            + " fila, no desplegar una migración")
    void guarda_un_eje_nuevo_y_lo_devuelve_con_su_id() {
        LimitDimension nuevo = LimitDimension.create("TELECONSULT", "Teleconsultas del periodo",
                MeasureKind.FLOW, null, null, LocalDate.of(2026, 4, 1),
                LocalDateTime.of(2026, 4, 1, 8, 0));

        LimitDimension guardado = repository.save(nuevo);
        entityManager.flush();
        entityManager.clear();

        assertThat(guardado.getId()).isNotNull();
        assertThat(repository.findById(guardado.getId())).get().satisfies(leido -> {
            assertThat(leido.getMeasureKind()).isEqualTo(MeasureKind.FLOW);
            assertThat(leido.getCode()).isEqualTo("TELECONSULT");
        });
    }

    @Test
    @DisplayName("R-LIMIT-03 · declarar un eje acumulativo sin días de enfriamiento muere en el"
            + " motor, no solo en el dominio")
    void un_eje_acumulativo_sin_enfriamiento_muere_en_el_motor() {
        assertViolates("chk_limit_dimensions_release_delay", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO limit_dimensions (code, name, measure_kind, sub_module_id,
                                                  release_delay_days, available_from,
                                                  created_date, enabled, version)
                    VALUES ('TELECONSULT', 'Teleconsultas', 'CUMULATIVE', NULL, NULL,
                            '2026-01-01', NOW(), true, 0)
                    """).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("dos ejes con el mismo código no pueden coexistir")
    void dos_ejes_con_el_mismo_codigo_no_pueden_coexistir() {
        assertThat(repository.existsByCode("ANIMAL")).isTrue();

        assertViolates("uq_limit_dimensions_code", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO limit_dimensions (code, name, measure_kind, sub_module_id,
                                                  release_delay_days, available_from,
                                                  created_date, enabled, version)
                    VALUES ('ANIMAL', 'Mascotas otra vez', 'CUMULATIVE', NULL, 30, '2026-01-01',
                            NOW(), true, 0)
                    """).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("el listado devuelve el catálogo completo ordenado por código")
    void el_listado_devuelve_el_catalogo_ordenado() {
        assertThat(repository.findAllOrderedByCode())
                .extracting(LimitDimension::getCode).contains("ANIMAL", "APPOINTMENT", "BRANCH",
                        "INVOICE", "OWNER", "STORAGE_GB", "TERMINAL", "USER")
                .isSortedAccordingTo(String::compareTo);
    }
}
