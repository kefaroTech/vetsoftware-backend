package com.vetsoftware.app.catalogitemlimit.infrastructure.persistence;

import static com.vetsoftware.app.testsupport.EngineConstraint.assertViolates;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimit;
import com.vetsoftware.app.catalogitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.catalogitemlimit.domain.LimitMode;
import com.vetsoftware.app.catalogitemlimit.domain.MeasureKind;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCatalogItemLimitRepository — el techo de fábrica contra MySQL real")
class CatalogItemLimitPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 8, 0);

    @Autowired
    private JpaCatalogItemLimitRepository repository;
    @Autowired
    private JpaLimitDimensionQueryPort limitDimensionQueryPort;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Resuelto, no sembrado: los ocho ejes llegan poblados por el changeset 313.
     */
    private Long ejeAnimal;

    /**
     * <b>No es {@code CORE} a proposito.</b> El changeset 313 ya siembra techos de
     * fabrica para {@code CORE} sobre {@code ANIMAL} y {@code OWNER}, y
     * {@code uq_catalog_item_limits} es (articulo, eje): sobre {@code CORE} el
     * {@code save} de esta rodaja chocaria contra la fila de la migracion y —peor—
     * los dos casos de {@code assertThatThrownBy} pasarian en verde por la clave
     * unica en vez de por el {@code CHECK} y la clave foranea compuesta que dicen
     * estar probando. {@code CLINICAL_HISTORY} es un articulo real del catalogo al
     * que 313 no le pone ningun techo, asi que aqui se puede afirmar «exactamente
     * uno».
     */
    private Long historiaClinica;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        historiaClinica = SchemaSeed.catalogItemId(entityManager, "CLINICAL_HISTORY");
        entityManager.flush();
        ejeAnimal = SchemaSeed.limitDimensionId(entityManager, "ANIMAL");
    }

    private CatalogItemLimit cienMascotas() {
        return CatalogItemLimit.create(historiaClinica, ejeAnimal, MeasureKind.CUMULATIVE,
                LimitMode.LIMITED, 100, null, LimitEnforcement.BLOCK, null, 80, LimitMode.FULL,
                null, CREADO);
    }

    @Test
    @DisplayName("guarda el techo de fábrica con su copia del tipo de medida y lo vuelve a leer")
    void guarda_el_techo_de_fabrica_y_lo_lee() {
        CatalogItemLimit guardado = repository.save(cienMascotas());
        entityManager.flush();
        entityManager.clear();

        assertThat(guardado.getId()).isNotNull();
        assertThat(repository.findByCatalogItemIdAndLimitDimensionId(historiaClinica, ejeAnimal))
                .get().satisfies(leido -> {
                    assertThat(leido.getLimitQuantity()).isEqualTo(100);
                    assertThat(leido.getMeasureKind()).isEqualTo(MeasureKind.CUMULATIVE);
                    assertThat(leido.getWarnThreshold()).isEqualTo(80);
                    assertThat(leido.getTrialMode()).isEqualTo(LimitMode.FULL);
                });
    }

    @Test
    @DisplayName("el puerto resuelve el eje con su tipo de medida, que es lo que evita declarar un"
            + " excedente sobre un acumulativo")
    void el_puerto_resuelve_el_eje_con_su_tipo_de_medida() {
        assertThat(limitDimensionQueryPort.findById(ejeAnimal)).get().satisfies(eje -> {
            assertThat(eje.code()).isEqualTo("ANIMAL");
            assertThat(eje.measureKind()).isEqualTo(MeasureKind.CUMULATIVE);
        });
    }

    @Test
    @DisplayName("R-LIMIT-22 · una copia de measure_kind que no case con el eje muere en la clave"
            + " foránea compuesta")
    void una_copia_de_measure_kind_que_no_case_muere_en_el_motor() {
        assertViolates("fk_catalog_item_limits_dimension", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO catalog_item_limits (catalog_item_id, limit_dimension_id,
                                                     measure_kind, mode, limit_quantity,
                                                     reset_period, enforcement,
                                                     overage_unit_amount, warn_threshold,
                                                     trial_mode, trial_limit_quantity,
                                                     created_date, enabled, version)
                    VALUES (:itemId, :dimensionId, 'FLOW', 'LIMITED', 100, 'MONTH', 'BLOCK', NULL,
                            80, 'FULL', NULL, NOW(), true, 0)
                    """).setParameter("itemId", historiaClinica)
                    .setParameter("dimensionId", ejeAnimal).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("R-LIMIT-12 · declarar OVERAGE sobre el eje acumulativo muere en el motor")
    void declarar_OVERAGE_sobre_el_eje_ANIMAL_muere_en_el_motor() {
        assertViolates("chk_catalog_item_limits_overage", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO catalog_item_limits (catalog_item_id, limit_dimension_id,
                                                     measure_kind, mode, limit_quantity,
                                                     reset_period, enforcement,
                                                     overage_unit_amount, warn_threshold,
                                                     trial_mode, trial_limit_quantity,
                                                     created_date, enabled, version)
                    VALUES (:itemId, :dimensionId, 'CUMULATIVE', 'LIMITED', 100, NULL, 'OVERAGE',
                            500.00, 80, 'FULL', NULL, NOW(), true, 0)
                    """).setParameter("itemId", historiaClinica)
                    .setParameter("dimensionId", ejeAnimal).executeUpdate();
            entityManager.flush();
        });
    }

    /**
     * <b>Este caso estaba vacio.</b> Guardaba una fila y comprobaba que habia una;
     * nunca intentaba el duplicado, asi que pasaba en verde con
     * {@code uq_catalog_item_limits} borrado del esquema. Lo que faltaba es la
     * segunda escritura, y tiene que ir en SQL nativo: por el repositorio, el
     * {@code save} de una entidad que ya tiene id seria un {@code UPDATE} de la
     * misma fila y no chocaria con nada.
     *
     * <p>
     * La segunda fila es valida en todo lo demas —{@code CUMULATIVE} case con el
     * eje, sin periodo de reinicio, sin excedente— para que la unica barandilla que
     * pueda pararla sea la unicidad de (articulo, eje).
     */
    @Test
    @DisplayName("dos techos de fábrica del mismo artículo sobre el mismo eje mueren en"
            + " uq_catalog_item_limits")
    void dos_techos_del_mismo_articulo_sobre_el_mismo_eje_no_coexisten() {
        repository.save(cienMascotas());
        entityManager.flush();

        assertThat(repository.existsByCatalogItemIdAndLimitDimensionId(historiaClinica, ejeAnimal))
                .isTrue();

        assertViolates("uq_catalog_item_limits", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO catalog_item_limits (catalog_item_id, limit_dimension_id,
                                                     measure_kind, mode, limit_quantity,
                                                     reset_period, enforcement,
                                                     overage_unit_amount, warn_threshold,
                                                     trial_mode, trial_limit_quantity,
                                                     created_date, enabled, version)
                    VALUES (:itemId, :dimensionId, 'CUMULATIVE', 'LIMITED', 50, NULL, 'BLOCK',
                            NULL, 70, 'FULL', NULL, NOW(), true, 0)
                    """).setParameter("itemId", historiaClinica)
                    .setParameter("dimensionId", ejeAnimal).executeUpdate();
            entityManager.flush();
        });
    }

    /**
     * R-LIMIT-12, sobre la siembra real del changeset 313. <b>El excedente es la
     * excepcion, no la regla</b>: solo {@code INVOICE} lo lleva, a 500 COP la
     * unidad, y todos los demas ejes bloquean. Nadie lo estaba afirmando, asi que
     * una fila de semilla con {@code OVERAGE} colada en cualquier otro eje —o el
     * importe cambiado— entraba sin que ninguna prueba se moviera, y eso es
     * facturar consumo que el cliente creia bloqueado.
     */
    @Test
    @DisplayName("R-LIMIT-12 · de los techos de fábrica sembrados, solo el eje de facturas lleva"
            + " excedente; el resto bloquea")
    void solo_el_eje_de_facturas_lleva_excedente_en_la_siembra() {
        List<Object[]> techos = techosDeFabricaSembrados();

        assertThat(techos).as("la siembra de techos del changeset 313").isNotEmpty()
                .filteredOn(fila -> "OVERAGE".equals(fila[1])).singleElement().satisfies(fila -> {
                    assertThat(fila[0]).isEqualTo("INVOICE");
                    assertThat((BigDecimal) fila[2]).isEqualByComparingTo("500.00");
                });

        assertThat(techos).filteredOn(fila -> !"INVOICE".equals(fila[0])).isNotEmpty()
                .allSatisfy(fila -> {
                    assertThat(fila[1]).as("modo de aplicación del eje %s", fila[0])
                            .isEqualTo("BLOCK");
                    assertThat(fila[2]).as("importe de excedente del eje %s", fila[0]).isNull();
                });
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> techosDeFabricaSembrados() {
        return entityManager.createNativeQuery("""
                SELECT ld.code, cil.enforcement, cil.overage_unit_amount
                  FROM catalog_item_limits cil
                  JOIN limit_dimensions ld ON ld.id = cil.limit_dimension_id
                 ORDER BY ld.code
                """).getResultList();
    }

    @Test
    @DisplayName("bajar el cupo de 100 a 80 se guarda y no toca el eje")
    void bajar_el_cupo_de_fabrica_se_guarda() {
        CatalogItemLimit guardado = repository.save(cienMascotas());
        guardado.update(LimitMode.LIMITED, 80, null, LimitEnforcement.BLOCK, null, 80,
                LimitMode.FULL, null);
        repository.save(guardado);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).get()
                .satisfies(leido -> assertThat(leido.getLimitQuantity()).isEqualTo(80));
    }
}
