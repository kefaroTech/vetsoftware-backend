package com.vetsoftware.app.entitlement.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * <b>R-LIMIT-39: la migración que repunta el contador rellena antes de retirar,
 * y el orden es la mitad de su corrección.</b>
 *
 * <p>
 * El changeset 314 lleva el contador de una lista cerrada de cuatro unidades
 * escritas a mano —{@code capacity_unit}— al catálogo de ejes. Su propio
 * comentario avisa de lo que pasa si alguien reordena los pasos, y es lo que
 * hace que este archivo exista: soltar {@code capacity_unit} <em>antes</em> de
 * rellenar {@code limit_dimension_id} <b>no rompe nada visible</b>. Deja todos
 * los contadores apuntando a {@code NULL}, con los datos reales intactos, y el
 * resultado es <b>todas las empresas con techo cero y sin un solo error en el
 * log</b>. Nadie lo estaba comprobando.
 *
 * <h2>Qué se puede afirmar sin volver a correr Liquibase al revés</h2>
 *
 * <p>
 * No se puede reordenar el changeset dentro de un test —el checksum es
 * inmutable, y reejecutarlo en otro orden sería probar otra migración—. Lo que
 * sí se puede es afirmar <b>las huellas que el orden correcto deja y el
 * incorrecto no</b>, que es exactamente lo que un trinquete necesita:
 *
 * <ol>
 * <li>Las tres columnas nuevas quedan {@code NOT NULL}. Con los pasos al revés
 * el {@code addNotNullConstraint} habría muerto sobre una columna llena de
 * nulos, así que verlas obligatorias es la prueba de que el relleno corrió
 * primero.
 * <li>{@code capacity_unit} ya no existe: la columna vieja se fue
 * <em>después</em>.
 * <li>Ningún contador quedó huérfano ni con el tipo de medida desatado de su
 * eje, y los que no son de flujo llevan el centinela. Es lo que el relleno
 * tenía que producir.
 * </ol>
 *
 * <p>
 * <b>Lo que este archivo NO comprueba</b>, y conviene decirlo: no ejercita la
 * precondición del changeset —la que falla en voz alta cuando un valor de
 * {@code capacity_unit} no tiene fila en {@code limit_dimensions}—, porque para
 * eso haría falta una base con datos anteriores a la migración.
 */
@DisplayName("314 · el repunte del contador al catálogo de ejes dejó las huellas del orden correcto")
class CapacityRepointMigrationIT extends AbstractDataJpaTest {

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    /**
     * La huella del paso 2. Si el relleno no hubiera corrido antes, estas columnas
     * seguirían siendo nulables —o la migración habría reventado— y los contadores
     * apuntarían a la nada.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"limit_dimension_id", "measure_kind", "period_key"})
    @DisplayName("las columnas del eje quedaron obligatorias, que es lo que el relleno previo hace"
            + " posible")
    void las_columnas_del_eje_quedaron_obligatorias(String columna) {
        assertThat(esNulable("company_capacities", columna))
                .as("company_capacities.%s admite nulos", columna).isEqualTo("NO");
    }

    @Test
    @DisplayName("la columna vieja capacity_unit se retiró, y se retiró después")
    void la_columna_vieja_se_retiro() {
        assertThat(columnasDe("company_capacities")).doesNotContain("capacity_unit").contains(
                "limit_dimension_id", "measure_kind", "period_key", "usage_reconciled_at");
    }

    /**
     * El resultado del relleno, sobre las filas reales. Cero es lo sano: un
     * contador que no resuelve su eje, o que copió un tipo de medida distinto del
     * que el eje declara, es un cupo que no se reinicia cuando debe.
     */
    @Test
    @DisplayName("ningún contador quedó sin eje ni con un tipo de medida desatado del suyo")
    void ningun_contador_quedo_sin_eje_ni_con_medida_desatada() {
        assertThat(cuantos("""
                SELECT COUNT(*)
                  FROM company_capacities cc
                  LEFT JOIN limit_dimensions ld
                         ON ld.id = cc.limit_dimension_id
                        AND ld.measure_kind = cc.measure_kind
                 WHERE ld.id IS NULL
                """)).as("contadores huérfanos o con el tipo de medida desatado de su eje")
                .isZero();
    }

    /**
     * El centinela (R-LIMIT-05). Los ejes que no son de flujo llevan
     * {@code 'ALLTIME'}, y ninguno lleva la clave vacía: en un índice único dos
     * {@code NULL} no chocan entre sí, así que una columna nulable dejaría caber
     * dos contadores del mismo eje para la misma empresa — uno actualizándose y
     * otro leyéndose.
     */
    @Test
    @DisplayName("los contadores que no son de flujo llevan el centinela, y ninguno la clave vacía")
    void los_contadores_que_no_son_de_flujo_llevan_el_centinela() {
        assertThat(cuantos("""
                SELECT COUNT(*)
                  FROM company_capacities
                 WHERE period_key IS NULL
                    OR period_key = ''
                    OR (measure_kind <> 'FLOW' AND period_key <> 'ALLTIME')
                    OR (measure_kind =  'FLOW' AND period_key =  'ALLTIME')
                """)).as("contadores con la clave de periodo mal puesta").isZero();
    }

    /**
     * Y el techo real del andamio, que es lo que un relleno saltado habría puesto a
     * cero. El contador de usuarios de la empresa sembrada tiene que seguir
     * apuntando al eje {@code USER} con su techo intacto.
     */
    @Test
    @DisplayName("el contador sembrado conserva su techo y su eje, no un cero silencioso")
    void el_contador_sembrado_conserva_su_techo_y_su_eje() {
        Object[] contador = (Object[]) entityManager.createNativeQuery("""
                SELECT ld.code, cc.measure_kind, cc.period_key, cc.limit_quantity
                  FROM company_capacities cc
                  JOIN limit_dimensions ld ON ld.id = cc.limit_dimension_id
                 WHERE cc.id = :id
                """).setParameter("id", SchemaSeed.CAPACITY_ID).getSingleResult();

        assertThat(contador[0]).isEqualTo("USER");
        assertThat(contador[1]).isEqualTo("STOCK");
        assertThat(contador[2]).isEqualTo("ALLTIME");
        assertThat(((Number) contador[3]).intValue()).as("el techo contratado, no un cero")
                .isEqualTo(2);
    }

    // ------------------------------------------------------------------ andamio

    private String esNulable(String tabla, String columna) {
        return (String) entityManager.createNativeQuery("""
                SELECT IS_NULLABLE FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :tabla
                   AND COLUMN_NAME = :columna
                """).setParameter("tabla", tabla).setParameter("columna", columna)
                .getSingleResult();
    }

    @SuppressWarnings("unchecked")
    private List<String> columnasDe(String tabla) {
        return entityManager.createNativeQuery("""
                SELECT COLUMN_NAME FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :tabla
                """).setParameter("tabla", tabla).getResultList();
    }

    private long cuantos(String consulta) {
        return ((Number) entityManager.createNativeQuery(consulta).getSingleResult()).longValue();
    }
}
