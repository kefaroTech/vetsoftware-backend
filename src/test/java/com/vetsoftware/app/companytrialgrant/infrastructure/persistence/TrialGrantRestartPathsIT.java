package com.vetsoftware.app.companytrialgrant.infrastructure.persistence;

import static com.vetsoftware.app.testsupport.EngineConstraint.assertViolates;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrant;
import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import com.vetsoftware.app.companytrialgrant.domain.TrialWindowRef;
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
 * <b>D-03, las cinco vías de reinicio de la prueba, cerradas de un golpe.</b>
 *
 * <p>
 * {@code uq_company_trial_grants_item (company_id, catalog_item_id)} es la
 * invariante entera de la decisión: <em>un artículo no se regala dos veces a la
 * misma empresa, jamás</em>. El changeset 302 lo dice literalmente y enumera lo
 * que cierra — reponer un módulo, cambiar de ciclo, migrar de tarifa, reactivar
 * tras suspender y recontratar tras cancelar—. <b>Nadie lo ejercitaba.</b> El
 * único caso que la tocaba
 * ({@code CompanyTrialGrantPersistenceIT#un_articulo_no_se_regala_dos_veces_a_la_misma_empresa})
 * ni siquiera probaba esta clave: la segunda concesión salía con la misma fecha
 * de fin y moría antes en {@code uq_company_trial_grants_line}.
 *
 * <p>
 * <b>Qué se rompe sin ella, y es el escenario que justifica la clase
 * entera:</b> quitar un módulo el día 29 de una prueba de 30 y reponerlo el 30
 * es software gratis indefinido — y <em>ninguna fila del modelo estaría
 * mal</em>. Sin una prueba que lo intente, borrar esta clave del esquema no
 * pondría rojo nada.
 *
 * <h2>La fecha de la segunda concesión no es casual</h2>
 *
 * <p>
 * Todas las tentativas de este archivo caen en una fecha de fin distinta de la
 * de la concesión original. Es deliberado y es la lección del defecto que se
 * acaba de corregir: con la misma fecha, la unicidad de línea
 * {@code (company_id, catalog_item_id, trial_end_date)} salta primero y el caso
 * pasaría en verde aunque D-03 no existiera.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("D-03 · las cinco vías de reinicio de la prueba mueren en uq_company_trial_grants_item")
class TrialGrantRestartPathsIT extends AbstractDataJpaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 9, 30);
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 9, 1, 8, 0);

    /** El contrato de repuesto, para el camino de recontratar tras cancelar. */
    private static final Long SEGUNDO_CONTRATO_ID = 9701L;

    @Autowired
    private JpaCompanyTrialGrantRepository repository;
    @Autowired
    private JpaTrialWindowQueryPort trialWindowQueryPort;
    @PersistenceContext
    private EntityManager entityManager;

    private Long ventanaId;
    private Long nucleo;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
        abrirVentana(INICIO, 30);
        ventanaId = trialWindowQueryPort.findOpenByCompanyId(SchemaSeed.COMPANY_ID).orElseThrow()
                .id();

        repository.save(CompanyTrialGrant.grant(
                new TrialWindowRef(ventanaId, SchemaSeed.COMPANY_ID, INICIO, FIN, true), nucleo,
                INICIO, 30, 30, TrialPolicyOutcome.LIMITED, SchemaSeed.QUOTE_ID, null, CREADA));
        entityManager.flush();
    }

    // ------------------------------------------------------------------ vía 1

    @Test
    @DisplayName("vía 1 · reponer el módulo quitado el día 29 no vuelve a regalarlo")
    void reponer_un_modulo_no_vuelve_a_regalarlo() {
        entityManager
                .createNativeQuery("UPDATE subscription_items SET effective_to = '2026-09-29'"
                        + " WHERE id = :itemId")
                .setParameter("itemId", SchemaSeed.SUBSCRIPTION_ITEM_ID).executeUpdate();
        entityManager.flush();

        assertViolates("uq_company_trial_grants_item",
                () -> intentarSegundaConcesion(ventanaId, FIN, "2026-09-29", 1, "2026-09-29"));
    }

    // ------------------------------------------------------------------ vía 2

    @Test
    @DisplayName("vía 2 · cambiar de ciclo de facturación no reabre la prueba del módulo")
    void cambiar_de_ciclo_no_reabre_la_prueba() {
        entityManager
                .createNativeQuery("UPDATE subscriptions SET billing_cycle = 'ANNUAL',"
                        + " version = version + 1 WHERE id = :contratoId")
                .setParameter("contratoId", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();
        entityManager.flush();

        assertViolates("uq_company_trial_grants_item",
                () -> intentarSegundaConcesion(ventanaId, FIN, "2026-09-20", 5, "2026-09-24"));
    }

    // ------------------------------------------------------------------ vía 3

    /**
     * La tarifa a la que se migra es {@code LISTA-2026-01}, la lista comercial real
     * que publican los changesets 310 y 311. No se inventa una aquí: firmar contra
     * una lista que no existe en el catálogo no es el escenario.
     */
    @Test
    @DisplayName("vía 3 · migrar de tarifa no reabre la prueba del módulo")
    void migrar_de_tarifa_no_reabre_la_prueba() {
        entityManager
                .createNativeQuery(
                        "UPDATE subscriptions SET price_list_id = (SELECT id FROM price_lists"
                                + " WHERE code = 'LISTA-2026-01'), version = version + 1"
                                + " WHERE id = :contratoId")
                .setParameter("contratoId", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();
        entityManager.flush();

        assertViolates("uq_company_trial_grants_item",
                () -> intentarSegundaConcesion(ventanaId, FIN, "2026-09-20", 5, "2026-09-24"));
    }

    // ------------------------------------------------------------------ vía 4

    @Test
    @DisplayName("vía 4 · reactivar tras suspender no reabre la prueba del módulo")
    void reactivar_tras_suspender_no_reabre_la_prueba() {
        cambiarEstadoDelContrato("READ_ONLY");
        cambiarEstadoDelContrato("ACTIVE");

        assertViolates("uq_company_trial_grants_item",
                () -> intentarSegundaConcesion(ventanaId, FIN, "2026-09-20", 5, "2026-09-24"));
    }

    // ------------------------------------------------------------------ vía 5

    /**
     * El camino más largo, y el que más se parece a lo que intentaría un cliente:
     * cancelar el contrato, cerrar la ventana, firmar otro y abrir una ventana
     * nueva. Todo eso es legítimo y el motor lo permite —
     * {@code uq_company_trial_windows_open} solo exige <em>una abierta</em>, no una
     * en la vida—. Lo que no se puede es volver a regalar el mismo artículo, y la
     * segunda concesión cuelga ya de la ventana nueva, con su fecha de fin propia.
     */
    @Test
    @DisplayName("vía 5 · recontratar tras cancelar no reabre la prueba del módulo, aunque la"
            + " ventana nueva sea legítima")
    void recontratar_tras_cancelar_no_reabre_la_prueba() {
        Long ventanaNueva = cancelarYRecontratar(LocalDate.of(2026, 9, 20));

        assertViolates("uq_company_trial_grants_item", () -> intentarSegundaConcesion(ventanaNueva,
                LocalDate.of(2026, 10, 19), "2026-09-20", 30, "2026-10-19"));
    }

    /**
     * <b>La variante del mismo día, que es la que un comercial probaría
     * primero.</b> Cancelar y volver a contratar sin dejar pasar un día abre una
     * ventana nueva —eso es correcto, y aquí se afirma que ocurre— pero <em>no</em>
     * abre una segunda prueba: la concesión original sigue siendo la única del
     * artículo, con su fecha de fin intacta.
     */
    @Test
    @DisplayName("cancelar y recontratar el mismo día abre ventana nueva pero no una segunda"
            + " prueba del mismo módulo")
    void cancelar_y_recontratar_el_mismo_dia_no_abre_segunda_prueba() {
        Long ventanaNueva = cancelarYRecontratar(LocalDate.of(2026, 9, 10));

        assertThat(ventanaNueva).as("la ventana nueva del contrato nuevo").isNotEqualTo(ventanaId);
        assertViolates("uq_company_trial_grants_item", () -> intentarSegundaConcesion(ventanaNueva,
                LocalDate.of(2026, 10, 9), "2026-09-10", 30, "2026-10-09"));

        entityManager.clear();
        assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID)).singleElement()
                .satisfies(unica -> {
                    assertThat(unica.getCatalogItemId()).isEqualTo(nucleo);
                    assertThat(unica.getTrialEndDate()).as("la prueba no se reinició")
                            .isEqualTo(FIN);
                });
    }

    // ------------------------------------------------------------------ andamio

    /**
     * La tentativa de segunda concesión. {@code trialEnd} tiene que ser la que
     * calcula {@code chk_company_trial_grants_end} —el menor entre «alta más sus
     * días menos uno» y el fin de la ventana— y tiene que ser distinta de la de la
     * concesión original, para que la unicidad de línea no se adelante.
     */
    private void intentarSegundaConcesion(Long ventana, LocalDate finDeVentana, String altaEn,
            int dias, String trialEnd) {
        entityManager.createNativeQuery("""
                INSERT INTO company_trial_grants (company_id, catalog_item_id, trial_window_id,
                                                  trial_window_end_date, granted_on, days_granted,
                                                  trial_end_date, policy_trial_days,
                                                  policy_trial_outcome, source_quote_id,
                                                  granting_amendment_id, consumed_at, outcome,
                                                  created_date, version)
                VALUES (:companyId, :itemId, :windowId, :windowEnd, :altaEn, :dias, :trialEnd, 30,
                        'LIMITED', :quoteId, NULL, NULL, NULL, NOW(), 0)
                """).setParameter("companyId", SchemaSeed.COMPANY_ID).setParameter("itemId", nucleo)
                .setParameter("windowId", ventana).setParameter("windowEnd", finDeVentana)
                .setParameter("altaEn", LocalDate.parse(altaEn)).setParameter("dias", dias)
                .setParameter("trialEnd", LocalDate.parse(trialEnd))
                .setParameter("quoteId", SchemaSeed.QUOTE_ID).executeUpdate();
        entityManager.flush();
    }

    private void abrirVentana(LocalDate desde, int dias) {
        entityManager.createNativeQuery("""
                INSERT INTO company_trial_windows (company_id, start_date, end_date, window_days,
                                                   source_quote_id, closed_at, created_date,
                                                   version)
                VALUES (:companyId, :desde, :hasta, :dias, :quoteId, NULL, NOW(), 0)
                """).setParameter("companyId", SchemaSeed.COMPANY_ID).setParameter("desde", desde)
                .setParameter("hasta", desde.plusDays(dias - 1L)).setParameter("dias", dias)
                .setParameter("quoteId", SchemaSeed.QUOTE_ID).executeUpdate();
        entityManager.flush();
    }

    private void cambiarEstadoDelContrato(String estado) {
        entityManager
                .createNativeQuery("UPDATE subscriptions SET status = :estado,"
                        + " version = version + 1 WHERE id = :contratoId")
                .setParameter("estado", estado)
                .setParameter("contratoId", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();
        entityManager.flush();
    }

    /**
     * Cancela el contrato, cierra la ventana viva y firma otro con su ventana
     * nueva. El orden importa: {@code uq_subscriptions_active_company} no deja dos
     * contratos vigentes a la vez y {@code uq_company_trial_windows_open} no deja
     * dos ventanas abiertas.
     *
     * @return el id de la ventana nueva
     */
    private Long cancelarYRecontratar(LocalDate elDia) {
        cambiarEstadoDelContrato("CANCELLED");
        entityManager
                .createNativeQuery("UPDATE company_trial_windows SET closed_at = :cierre,"
                        + " version = version + 1 WHERE id = :ventanaId")
                .setParameter("cierre", elDia.atTime(12, 0)).setParameter("ventanaId", ventanaId)
                .executeUpdate();
        entityManager.flush();

        entityManager.createNativeQuery("""
                INSERT INTO subscriptions (id, subscription_number, company_id, quote_id,
                                           price_list_id, billing_cycle, status, start_date,
                                           trial_end_date, current_period_start,
                                           current_period_end, next_billing_date,
                                           commitment_end_date, grace_days, past_due_since,
                                           auto_renew, created_date, enabled, version)
                SELECT :id, 'SUS-TEST-009002', :companyId, NULL, s.price_list_id, 'MONTHLY',
                       'ACTIVE', :desde, NULL, :desde, :hasta, :hasta, NULL, 5, NULL, true,
                       NOW(), true, 0
                  FROM subscriptions s
                 WHERE s.id = :origen
                """).setParameter("id", SEGUNDO_CONTRATO_ID)
                .setParameter("companyId", SchemaSeed.COMPANY_ID).setParameter("desde", elDia)
                .setParameter("hasta", elDia.plusMonths(1))
                .setParameter("origen", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();
        entityManager.flush();

        abrirVentana(elDia, 30);
        entityManager.clear();
        return trialWindowQueryPort.findOpenByCompanyId(SchemaSeed.COMPANY_ID).orElseThrow().id();
    }
}
