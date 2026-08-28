package com.vetsoftware.app.subscription.infrastructure.persistence;

import static com.vetsoftware.app.testsupport.EngineConstraint.assertViolates;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * <b>El fin de prueba en la línea de contrato: la comprobación que nadie
 * ejercitaba.</b>
 *
 * <p>
 * {@code chk_subscription_items_trial_end} ata las dos mitades de un mismo
 * hecho: una línea en prueba <em>tiene</em> fecha de fin, y una que no está en
 * prueba <em>no la tiene</em>. Sin la primera mitad existe la prueba eterna
 * —una línea {@code charge_mode = 'TRIAL'} sin fecha no vence nunca y ningún
 * barrido la ve—; sin la segunda, una línea de pago arrastra una fecha que la
 * engancha, por {@code fk_subscription_items_trial_grant}, a una concesión que
 * no le corresponde.
 *
 * <h2>Por qué el caso de la línea de pago necesita una concesión sembrada</h2>
 *
 * <p>
 * Es la trampa de este bloque entero, y aquí está a la vista. Una línea de pago
 * con fecha de fin viola <em>dos</em> cosas: la comprobación que este archivo
 * dice probar y la clave foránea triple hacia {@code company_trial_grants}, que
 * en MySQL solo se evalúa cuando ninguna de sus columnas es nula. Sin sembrar
 * la concesión, el caso pasaría en verde por la clave foránea y seguiría
 * pasando con la comprobación borrada del esquema. Se siembra, y así la única
 * barandilla que puede parar la sentencia es la que se está probando.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("subscription_items — el fin de prueba de la línea contra MySQL real")
class SubscriptionTrialEndPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDate INICIO_VENTANA = LocalDate.of(2026, 9, 1);
    private static final LocalDate FIN_VENTANA = LocalDate.of(2026, 9, 30);
    private static final LocalDate FIN_TEMPRANO = LocalDate.of(2026, 9, 24);

    private static final Long LINEA_AGENDA_ID = 9721L;
    private static final Long LINEA_HISTORIA_ID = 9722L;

    @PersistenceContext
    private EntityManager entityManager;

    private Long agenda;
    private Long historia;
    private Long ventanaId;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        agenda = SchemaSeed.catalogItemId(entityManager, "SCHEDULING");
        historia = SchemaSeed.catalogItemId(entityManager, "CLINICAL_HISTORY");
        ventanaId = abrirVentana();
    }

    @Nested
    @DisplayName("R-TRIAL-06 · la comprobación del fin de prueba en la línea")
    class ComprobacionDelFinDePrueba {

        /**
         * La prueba eterna. Sin fecha de fin, la línea nunca vence: el barrido de
         * vencimientos busca por {@code trial_end_date} y esta fila no aparece en
         * ninguna ejecución, ni hoy ni dentro de tres años.
         */
        @Test
        @DisplayName("una línea en prueba sin fecha de fin —prueba eterna— muere en"
                + " chk_subscription_items_trial_end")
        void una_linea_en_prueba_sin_fecha_de_fin_muere_en_el_motor() {
            assertViolates("chk_subscription_items_trial_end",
                    () -> insertarLinea(LINEA_AGENDA_ID, agenda, "TRIAL", null));
        }

        /**
         * La otra mitad. La concesión está sembrada a propósito —ver el javadoc de la
         * clase—: sin ella este caso moriría en
         * {@code fk_subscription_items_trial_grant} y no probaría nada.
         */
        @Test
        @DisplayName("una línea de pago con fecha de fin de prueba muere en"
                + " chk_subscription_items_trial_end, no en la clave foránea")
        void una_linea_de_pago_con_fecha_de_fin_muere_en_el_motor() {
            sembrarConcesion(agenda, FIN_VENTANA);

            assertViolates("chk_subscription_items_trial_end",
                    () -> insertarLinea(LINEA_AGENDA_ID, agenda, "PAID", FIN_VENTANA));
        }

        /**
         * El par correcto entra: es lo que demuestra que la comprobación no es un veto.
         */
        @Test
        @DisplayName("una línea en prueba con su fecha de fin, y una de pago sin ella, entran las"
                + " dos")
        void el_par_correcto_entra() {
            sembrarConcesion(agenda, FIN_VENTANA);
            insertarLinea(LINEA_AGENDA_ID, agenda, "TRIAL", FIN_VENTANA);
            insertarLinea(LINEA_HISTORIA_ID, historia, "PAID", null);
            entityManager.clear();

            assertThat(finDePruebaDeLaLinea(LINEA_AGENDA_ID)).isEqualTo(FIN_VENTANA);
            assertThat(finDePruebaDeLaLinea(LINEA_HISTORIA_ID)).isNull();
        }
    }

    @Nested
    @DisplayName("La fecha de prueba del contrato no la mueve una baja de línea")
    class LaBajaNoMueveLaFechaDelContrato {

        /**
         * <b>Dar de baja el módulo que vencía más tarde no acorta la prueba del
         * contrato.</b> El escenario: dos módulos en prueba, uno hasta el 24 y otro
         * hasta el 30, y el contrato en {@code TRIALING} con fin el 30. Se quita el que
         * vencía el 30 —el que fija la fecha del contrato— y la fecha del contrato
         * tiene que quedarse donde estaba.
         *
         * <p>
         * <b>Qué vigila de verdad.</b> Hoy no hay código que recalcule
         * {@code subscriptions.trial_end_date} al dar de baja una línea, así que este
         * caso pasa por construcción; su valor es de trinquete. La tentación de
         * «recalcular el fin de prueba como el máximo de las líneas vivas» es razonable
         * a primera vista y rompe dos cosas a la vez: mueve una fecha que el cliente
         * firmó, y como ese mismo valor está copiado dentro de cada concesión y atado
         * por clave foránea con {@code ON UPDATE RESTRICT}, la mitad de las bajas
         * empezarían a morir en el motor sin explicación. Este caso se pone rojo el día
         * que alguien lo intente.
         */
        @Test
        @DisplayName("dar de baja el módulo que vencía más tarde no mueve la fecha de prueba del"
                + " contrato")
        void dar_de_baja_el_modulo_que_vencia_mas_tarde_no_mueve_la_fecha_del_contrato() {
            ponerElContratoEnPrueba();
            sembrarConcesion(agenda, FIN_VENTANA);
            sembrarConcesion(historia, FIN_TEMPRANO);
            insertarLinea(LINEA_AGENDA_ID, agenda, "TRIAL", FIN_VENTANA);
            insertarLinea(LINEA_HISTORIA_ID, historia, "TRIAL", FIN_TEMPRANO);
            entityManager.flush();

            darDeBaja(LINEA_AGENDA_ID);
            entityManager.clear();

            assertThat(finDePruebaDelContrato())
                    .as("la fecha de prueba que el cliente firmó, tras quitar el módulo que la"
                            + " sostenía")
                    .isEqualTo(FIN_VENTANA);
            assertThat(finDePruebaDeLaLinea(LINEA_HISTORIA_ID))
                    .as("la línea que se queda conserva la suya").isEqualTo(FIN_TEMPRANO);
        }
    }

    // ------------------------------------------------------------------ andamio

    private Long abrirVentana() {
        entityManager.createNativeQuery("""
                INSERT INTO company_trial_windows (company_id, start_date, end_date, window_days,
                                                   source_quote_id, closed_at, created_date,
                                                   version)
                VALUES (:companyId, :desde, :hasta, 30, :quoteId, NULL, NOW(), 0)
                """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("desde", INICIO_VENTANA).setParameter("hasta", FIN_VENTANA)
                .setParameter("quoteId", SchemaSeed.QUOTE_ID).executeUpdate();
        entityManager.flush();
        return ((Number) entityManager
                .createNativeQuery("SELECT id FROM company_trial_windows WHERE company_id ="
                        + " :companyId AND closed_at IS NULL")
                .setParameter("companyId", SchemaSeed.COMPANY_ID).getSingleResult()).longValue();
    }

    /**
     * La concesión que sostiene la clave foránea triple de la línea. Los días se
     * calculan hacia atrás desde la fecha de fin pedida para que
     * {@code chk_company_trial_grants_end} cuadre.
     */
    private void sembrarConcesion(Long catalogItemId, LocalDate finDePrueba) {
        entityManager.createNativeQuery("""
                INSERT INTO company_trial_grants (company_id, catalog_item_id, trial_window_id,
                                                  trial_window_end_date, granted_on, days_granted,
                                                  trial_end_date, policy_trial_days,
                                                  policy_trial_outcome, source_quote_id,
                                                  granting_amendment_id, consumed_at, outcome,
                                                  created_date, version)
                VALUES (:companyId, :itemId, :windowId, :windowEnd, :desde, :dias, :fin, 30,
                        'LIMITED', :quoteId, NULL, NULL, NULL, NOW(), 0)
                """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("itemId", catalogItemId).setParameter("windowId", ventanaId)
                .setParameter("windowEnd", FIN_VENTANA).setParameter("desde", INICIO_VENTANA)
                .setParameter("dias",
                        (int) ChronoUnit.DAYS.between(INICIO_VENTANA, finDePrueba) + 1)
                .setParameter("fin", finDePrueba).setParameter("quoteId", SchemaSeed.QUOTE_ID)
                .executeUpdate();
        entityManager.flush();
    }

    private void insertarLinea(Long id, Long catalogItemId, String chargeMode,
            LocalDate finDePrueba) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_items (id, company_id, subscription_id, catalog_item_id,
                                                item_code, item_name, item_type, capacity_unit,
                                                included_quantity, tax_treatment, quantity,
                                                unit_amount, tax_rate, tier_min, tier_max,
                                                months_in_cycle, charge_mode, trial_eligibility,
                                                max_trial_days, trial_end_date, activation_path,
                                                billing_effect, effective_from, effective_to,
                                                origin, succeeds_item_id, created_amendment_id,
                                                ended_amendment_id, created_date, enabled, version)
                VALUES (:id, :companyId, :subscriptionId, :itemId, 'LINEA', 'Linea de prueba',
                        'MODULE', NULL, 0, 'TAXED', 1, 50000.00, 19.00, 1, NULL, 1, :chargeMode,
                        'ELIGIBLE', 30, :fin, 'QUOTE', 'NONE', :desde, NULL, 'ADDON', NULL, NULL,
                        NULL, NOW(), true, 0)
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("subscriptionId", SchemaSeed.SUBSCRIPTION_ID)
                .setParameter("itemId", catalogItemId).setParameter("chargeMode", chargeMode)
                .setParameter("fin", finDePrueba).setParameter("desde", INICIO_VENTANA)
                .executeUpdate();
        entityManager.flush();
    }

    private void ponerElContratoEnPrueba() {
        entityManager
                .createNativeQuery("UPDATE subscriptions SET status = 'TRIALING',"
                        + " trial_end_date = :fin, version = version + 1 WHERE id = :contratoId")
                .setParameter("fin", FIN_VENTANA)
                .setParameter("contratoId", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();
        entityManager.flush();
    }

    private void darDeBaja(Long lineaId) {
        entityManager
                .createNativeQuery("UPDATE subscription_items SET effective_to = :fin,"
                        + " version = version + 1 WHERE id = :lineaId")
                .setParameter("fin", FIN_TEMPRANO).setParameter("lineaId", lineaId).executeUpdate();
        entityManager.flush();
    }

    private LocalDate finDePruebaDeLaLinea(Long lineaId) {
        return (LocalDate) entityManager.createNativeQuery(
                "SELECT trial_end_date FROM subscription_items WHERE id = :lineaId",
                LocalDate.class).setParameter("lineaId", lineaId).getSingleResult();
    }

    private LocalDate finDePruebaDelContrato() {
        return (LocalDate) entityManager
                .createNativeQuery(
                        "SELECT trial_end_date FROM subscriptions WHERE id = :contratoId",
                        LocalDate.class)
                .setParameter("contratoId", SchemaSeed.SUBSCRIPTION_ID).getSingleResult();
    }
}
