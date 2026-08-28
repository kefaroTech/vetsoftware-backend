package com.vetsoftware.app.accountingperiod.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Los disparadores del changeset 346, contra MySQL real.
 *
 * <p>
 * <b>Por que existe esta clase.</b> Los disparadores son lo unico que impide
 * escribir contra un mes contable ya cerrado, y hasta hoy no los ejercitaba
 * nadie: las rodajas de {@code revenue_recognition_lines} y de
 * {@code accounting_exports} siembran siempre periodos {@code OPEN}, asi que el
 * camino que importa —el periodo cerrado— no se recorria nunca.
 *
 * <p>
 * <b>Y es el fallo mas caro de los que pueden quedarse sin red, porque es
 * silencioso por definicion.</b> Si el disparador desaparece, nada salta: se
 * imputa un ingreso a un mes ya declarado y el desajuste aparece meses despues,
 * cuando la contabilidad no cuadra y ya no hay forma de saber que fila lo hizo.
 * Un {@code CHECK} no puede sostener esta regla —el manual de MySQL prohibe
 * subconsultas y columnas de otra tabla dentro de un {@code CHECK}—, asi que o
 * lo prueba una rodaja contra el motor o no lo prueba nada.
 *
 * <p>
 * <b>Cada rechazo va emparejado con su control en verde.</b> El caso que cierra
 * el periodo y ve fallar la escritura no demuestra nada por si solo: fallaria
 * igual si el andamio estuviera roto y la fila no entrase por cualquier otro
 * motivo. Por eso cada {@code @Nested} tiene primero un caso que escribe la
 * MISMA fila contra el periodo abierto y la ve entrar. La unica diferencia
 * entre los dos es el estado del periodo, y solo por eso el rechazo se le puede
 * atribuir al disparador.
 *
 * <p>
 * <b>Ids del rango 9310 y meses de 2031</b>, que no usa ninguna otra rodaja
 * ({@code revenuerecognitionline} usa 8420 y 2028, la conciliacion externa 2026
 * y la de periodos 2027). El {@code 2026-08} que siembra el changeset 362 se
 * deja en paz salvo en el unico caso que necesita quedarse sin periodos
 * abiertos, y aun ahi solo dentro de su transaccion, que se deshace al salir.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("Disparadores de periodo contable (346) — un mes cerrado no admite escrituras")
class AccountingPeriodTriggerIT extends AbstractDataJpaTest {

    /** El que sirve de control: se queda {@code OPEN} todo el rato. */
    private static final String ABIERTO = "2031-01";

    /**
     * El que se cierra en mitad del caso para ver que el disparador para la
     * escritura.
     */
    private static final String CERRADO = "2031-02";

    /** El unico {@code OPEN} que siembra Liquibase (changeset 362). */
    private static final String SEMILLA = "2026-08";

    private static final Long PERIODO_ABIERTO_ID = 9310L;
    private static final Long PERIODO_CERRADO_ID = 9311L;
    private static final Long CARGO_ID = 9312L;

    private static final String MENSAJE_PERIODO = "periodo contable no abierto";

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void escenario() {
        SchemaSeed.seed(entityManager);
        periodoAbierto(PERIODO_ABIERTO_ID, ABIERTO);
        periodoAbierto(PERIODO_CERRADO_ID, CERRADO);
        cargo();
        entityManager.flush();
    }

    @Nested
    @DisplayName("trg_rrl_bi_period_open — el libro de ingreso")
    class LibroDeIngreso {

        @Test
        @DisplayName("contra un periodo abierto el renglon entra: el andamio sabe escribir")
        void contra_un_periodo_abierto_el_renglon_entra() {
            assertThatCode(() -> renglon(9320L, CERRADO)).doesNotThrowAnyException();

            assertThat(cuentaRenglones(CERRADO)).isEqualTo(1L);
        }

        @Test
        @DisplayName("cerrado el periodo, el mismo renglon se rechaza en el motor")
        void cerrado_el_periodo_el_mismo_renglon_se_rechaza() {
            cerrar(CERRADO);

            assertThatThrownBy(() -> renglon(9321L, CERRADO)).rootCause()
                    .hasMessageContaining(MENSAJE_PERIODO);
        }

        @Test
        @DisplayName("un periodo en cierre blando tampoco admite el renglon")
        void un_periodo_en_cierre_blando_tampoco_admite_el_renglon() {
            // SOFT_CLOSED no es LOCKED, pero el disparador exige OPEN y no
            // "distinto de LOCKED": el cierre blando ya para la escritura.
            cerrarBlando(CERRADO);

            assertThatThrownBy(() -> renglon(9322L, CERRADO)).rootCause()
                    .hasMessageContaining(MENSAJE_PERIODO);
        }
    }

    @Nested
    @DisplayName("trg_rrl_bu_period_open — reimputar un renglon ya escrito")
    class ReimputarElRenglon {

        @Test
        @DisplayName("mover el renglon a otro periodo abierto se permite")
        void mover_el_renglon_a_otro_periodo_abierto_se_permite() {
            renglon(9330L, ABIERTO);

            assertThatCode(() -> reimputar(9330L, CERRADO)).doesNotThrowAnyException();

            assertThat(cuentaRenglones(CERRADO)).isEqualTo(1L);
        }

        @Test
        @DisplayName("mover el renglon a un periodo ya declarado se rechaza")
        void mover_el_renglon_a_un_periodo_ya_declarado_se_rechaza() {
            // Este es el hecho tardio del enunciado: la fila ya existe y alguien la
            // reimputa al mes que ya se declaro. Sin el BEFORE UPDATE, el INSERT
            // estaria vigilado y la correccion posterior no.
            renglon(9331L, ABIERTO);
            cerrar(CERRADO);

            assertThatThrownBy(() -> reimputar(9331L, CERRADO)).rootCause()
                    .hasMessageContaining(MENSAJE_PERIODO);
        }
    }

    @Nested
    @DisplayName("trg_accounting_exports_bi_period_open — la exportacion al contador")
    class ExportacionContable {

        @Test
        @DisplayName("contra un periodo abierto la exportacion entra")
        void contra_un_periodo_abierto_la_exportacion_entra() {
            assertThatCode(() -> exportacion(9340L, CERRADO, 1)).doesNotThrowAnyException();

            assertThat(cuentaExportaciones(CERRADO)).isEqualTo(1L);
        }

        @Test
        @DisplayName("cerrado el periodo, la misma exportacion se rechaza en el motor")
        void cerrado_el_periodo_la_misma_exportacion_se_rechaza() {
            cerrar(CERRADO);

            assertThatThrownBy(() -> exportacion(9341L, CERRADO, 1)).rootCause()
                    .hasMessageContaining(MENSAJE_PERIODO);
        }

        @Test
        @DisplayName("marcar entregada una exportacion de un periodo cerrado se rechaza")
        void marcar_entregada_una_exportacion_de_un_periodo_cerrado_se_rechaza() {
            // El BEFORE UPDATE alcanza incluso a una edicion que NO toca period_key:
            // el disparador lee NEW.period_key, que en un UPDATE parcial conserva el
            // valor viejo. Entregar el fichero de un mes que se cerro entre medias
            // queda bloqueado, y eso es deliberado.
            exportacion(9342L, CERRADO, 1);
            cerrar(CERRADO);

            assertThatThrownBy(() -> marcarEntregada(9342L)).rootCause()
                    .hasMessageContaining(MENSAJE_PERIODO);
        }
    }

    @Nested
    @DisplayName("trg_accounting_periods_bu_guard — la guarda del propio periodo")
    class GuardaDelPeriodo {

        @Test
        @DisplayName("un periodo declarado no se reabre, aunque el CHECK lo permita")
        void un_periodo_declarado_no_se_reabre() {
            // El changeset 365 dejo escrito que chk_accounting_periods_closure SI
            // admite la fila de una reapertura (status OPEN con reopened_at y
            // closed_at informados). O sea que la regla "un periodo declarado no se
            // reabre" NO la sostiene ninguna constraint: la sostiene solo este
            // disparador, y este es el unico caso que lo demuestra.
            cerrar(CERRADO);

            assertThatThrownBy(() -> reabrir(CERRADO)).rootCause()
                    .hasMessageContaining("un periodo declarado no se reabre");
        }

        @Test
        @DisplayName("cerrar deja de estar permitido cuando seria el ultimo periodo abierto")
        void cerrar_el_ultimo_periodo_abierto_se_rechaza() {
            // Se cierran los otros dos para que la semilla del 362 quede sola. Con
            // dos abiertos cerrar uno funciona -lo demuestran los casos de arriba-,
            // asi que el rechazo de aqui solo puede venir del recuento.
            cerrar(ABIERTO);
            cerrar(CERRADO);

            assertThatThrownBy(() -> cerrar(SEMILLA)).rootCause()
                    .hasMessageContaining("no queda ningun periodo abierto");
        }
    }

    private void periodoAbierto(Long id, String clave) {
        entityManager.createNativeQuery("""
                INSERT INTO accounting_periods (id, period_key, status, created_date, version)
                VALUES (:id, :clave, 'OPEN', NOW(6), 0)
                """).setParameter("id", id).setParameter("clave", clave).executeUpdate();
    }

    private void cerrar(String clave) {
        entityManager.createNativeQuery("""
                UPDATE accounting_periods
                   SET status = 'LOCKED', closed_at = NOW(6), closed_by_system_user_id = :usuario
                 WHERE period_key = :clave
                """).setParameter("usuario", SchemaSeed.SYSTEM_USER_ID).setParameter("clave", clave)
                .executeUpdate();
        entityManager.flush();
    }

    private void cerrarBlando(String clave) {
        entityManager.createNativeQuery("""
                UPDATE accounting_periods
                   SET status = 'SOFT_CLOSED', closed_at = NOW(6),
                       closed_by_system_user_id = :usuario
                 WHERE period_key = :clave
                """).setParameter("usuario", SchemaSeed.SYSTEM_USER_ID).setParameter("clave", clave)
                .executeUpdate();
        entityManager.flush();
    }

    private void reabrir(String clave) {
        entityManager.createNativeQuery("""
                UPDATE accounting_periods
                   SET status = 'OPEN', reopened_at = NOW(6),
                       reopened_by_system_user_id = :usuario,
                       reopened_reason = 'Llego una factura de proveedor con fecha del mes'
                 WHERE period_key = :clave
                """).setParameter("usuario", SchemaSeed.SYSTEM_USER_ID).setParameter("clave", clave)
                .executeUpdate();
        entityManager.flush();
    }

    /**
     * El cargo del que cuelga el reconocimiento, solo para satisfacer
     * {@code fk_rrl_charge}. {@code ONE_TIME} y no {@code RECURRING} porque
     * {@code chk_subscription_charges_recurring_item} exigiria una linea de
     * contrato que este andamio no monta.
     */
    private void cargo() {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_charges (id, company_id, subscription_id, charge_type,
                        description, service_period_start, service_period_end, quantity,
                        unit_amount, subtotal_amount, tax_rate, tax_treatment, status,
                        created_date)
                VALUES (:id, :empresa, :contrato, 'ONE_TIME', 'Cargo de andamio', '2031-01-01',
                        '2031-01-31', 1.000, 100000.00, 100000.00, 0.00, 'EXCLUDED', 'PENDING',
                        NOW())
                """).setParameter("id", CARGO_ID).setParameter("empresa", SchemaSeed.COMPANY_ID)
                .setParameter("contrato", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();
    }

    private void renglon(Long id, String postingPeriod) {
        entityManager.createNativeQuery("""
                INSERT INTO revenue_recognition_lines (id, company_id, charge_id, period_key,
                        posting_period, recognized_amount, method, created_date)
                VALUES (:id, :empresa, :cargo, :periodo, :registro, :importe,
                        'STRAIGHT_LINE_DAYS', NOW(6))
                """).setParameter("id", id).setParameter("empresa", SchemaSeed.COMPANY_ID)
                .setParameter("cargo", CARGO_ID).setParameter("periodo", postingPeriod)
                .setParameter("registro", postingPeriod)
                .setParameter("importe", new BigDecimal("100000.00")).executeUpdate();
        entityManager.flush();
    }

    private void reimputar(Long id, String nuevoPostingPeriod) {
        entityManager.createNativeQuery("""
                UPDATE revenue_recognition_lines SET posting_period = :registro WHERE id = :id
                """).setParameter("registro", nuevoPostingPeriod).setParameter("id", id)
                .executeUpdate();
        entityManager.flush();
    }

    private void exportacion(Long id, String periodKey, int intento) {
        entityManager.createNativeQuery("""
                INSERT INTO accounting_exports (id, period_key, export_kind, attempt_number,
                        status, generated_at, generated_by_system_user_id, total_debit,
                        total_credit, totals_hash, file_ref, created_date, version)
                VALUES (:id, :periodo, 'JOURNAL_SUMMARY', :intento, 'GENERATED', NOW(6),
                        :usuario, 100000.00, 100000.00, :huella,
                        's3://exportaciones/andamio.csv', NOW(6), 0)
                """).setParameter("id", id).setParameter("periodo", periodKey)
                .setParameter("intento", intento).setParameter("usuario", SchemaSeed.SYSTEM_USER_ID)
                .setParameter("huella", "a".repeat(64)).executeUpdate();
        entityManager.flush();
    }

    private void marcarEntregada(Long id) {
        entityManager.createNativeQuery("""
                UPDATE accounting_exports SET status = 'DELIVERED', delivered_at = NOW(6)
                 WHERE id = :id
                """).setParameter("id", id).executeUpdate();
        entityManager.flush();
    }

    private Long cuentaRenglones(String postingPeriod) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM revenue_recognition_lines WHERE posting_period = :registro
                """).setParameter("registro", postingPeriod).getSingleResult()).longValue();
    }

    private Long cuentaExportaciones(String periodKey) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM accounting_exports WHERE period_key = :periodo
                """).setParameter("periodo", periodKey).getSingleResult()).longValue();
    }
}
