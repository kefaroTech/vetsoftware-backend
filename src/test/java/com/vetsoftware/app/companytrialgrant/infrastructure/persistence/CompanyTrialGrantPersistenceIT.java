package com.vetsoftware.app.companytrialgrant.infrastructure.persistence;

import static com.vetsoftware.app.testsupport.EngineConstraint.assertViolates;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrant;
import com.vetsoftware.app.companytrialgrant.domain.TrialOutcome;
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

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyTrialGrantRepository — las concesiones contra MySQL real")
class CompanyTrialGrantPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 9, 30);
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 9, 1, 8, 0);

    @Autowired
    private JpaCompanyTrialGrantRepository repository;
    @Autowired
    private JpaTrialWindowQueryPort trialWindowQueryPort;
    @PersistenceContext
    private EntityManager entityManager;

    private Long ventanaId;

    /** Resuelto, no sembrado: el articulo CORE llega del changeset 308. */
    private Long nucleo;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
        entityManager.createNativeQuery("""
                INSERT INTO company_trial_windows (company_id, start_date, end_date, window_days,
                                                   source_quote_id, closed_at, created_date,
                                                   version)
                VALUES (:companyId, '2026-09-01', '2026-09-30', 30, :quoteId, NULL, NOW(), 0)
                """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("quoteId", SchemaSeed.QUOTE_ID).executeUpdate();
        entityManager.flush();
        ventanaId = trialWindowQueryPort.findOpenByCompanyId(SchemaSeed.COMPANY_ID).orElseThrow()
                .id();
    }

    private TrialWindowRef ventana() {
        return new TrialWindowRef(ventanaId, SchemaSeed.COMPANY_ID, INICIO, FIN, true);
    }

    @Test
    @DisplayName("R-TRIAL-03 · un módulo concedido el 16 de septiembre hereda el fin de la ventana:"
            + " 15 días, no 30, y la restricción del motor lo confirma")
    void un_modulo_concedido_a_mitad_de_ventana_hereda_el_fin_de_la_ventana() {
        CompanyTrialGrant concesion = repository
                .save(CompanyTrialGrant.grant(ventana(), nucleo, LocalDate.of(2026, 9, 16), 30, 30,
                        TrialPolicyOutcome.LIMITED, SchemaSeed.QUOTE_ID, null, CREADA));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByCompanyIdAndCatalogItemId(SchemaSeed.COMPANY_ID, nucleo)).get()
                .satisfies(leida -> {
                    assertThat(leida.getTrialEndDate()).isEqualTo(FIN);
                    assertThat(leida.effectiveDays()).isEqualTo(15);
                    assertThat(leida.getPolicyTrialOutcome()).isEqualTo(TrialPolicyOutcome.LIMITED);
                });
        assertThat(concesion.getId()).isNotNull();
    }

    /**
     * <b>La segunda concesion tenia que caer en otra fecha, y ese es el
     * arreglo.</b> Antes salia con {@code trial_end_date = '2026-09-30'}, la misma
     * que la primera, asi que violaba <em>tambien</em>
     * {@code uq_company_trial_grants_line (company_id, catalog_item_id,
     * trial_end_date)}. Con dos unicidades candidatas el caso pasaba en verde
     * aunque la invariante de D-03 no existiera: bastaba con que saltara la otra.
     *
     * <p>
     * Concedida el 20 con 5 dias, {@code chk_company_trial_grants_end} calcula el
     * 24 de septiembre. La clave de linea ya no puede chocar, y la unica barandilla
     * que queda en pie es la que sostiene D-03:
     * {@code uq_company_trial_grants_item (company_id, catalog_item_id)}.
     */
    @Test
    @DisplayName("R-TRIAL-04 · un artículo no se regala dos veces a la misma empresa: la segunda"
            + " concesión muere en uq_company_trial_grants_item")
    void un_articulo_no_se_regala_dos_veces_a_la_misma_empresa() {
        repository.save(CompanyTrialGrant.grant(ventana(), nucleo, INICIO, 30, 30,
                TrialPolicyOutcome.LIMITED, SchemaSeed.QUOTE_ID, null, CREADA));
        entityManager.flush();

        assertViolates("uq_company_trial_grants_item", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO company_trial_grants (company_id, catalog_item_id, trial_window_id,
                                                      trial_window_end_date, granted_on,
                                                      days_granted, trial_end_date,
                                                      policy_trial_days, policy_trial_outcome,
                                                      source_quote_id, granting_amendment_id,
                                                      consumed_at, outcome, created_date, version)
                    VALUES (:companyId, :itemId, :windowId, '2026-09-30', '2026-09-20', 5,
                            '2026-09-24', 30, 'LIMITED', :quoteId, NULL, NULL, NULL, NOW(), 0)
                    """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                    .setParameter("itemId", nucleo).setParameter("windowId", ventanaId)
                    .setParameter("quoteId", SchemaSeed.QUOTE_ID).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("R-TRIAL-05 · estirar la ventana con concesiones colgando muere en el motor: la"
            + " clave va RESTRICT también al actualizar")
    void estirar_la_ventana_con_concesiones_vivas_muere_en_el_motor() {
        repository.save(CompanyTrialGrant.grant(ventana(), nucleo, INICIO, 30, 30,
                TrialPolicyOutcome.LIMITED, SchemaSeed.QUOTE_ID, null, CREADA));
        entityManager.flush();

        assertViolates("fk_company_trial_grants_window", () -> {
            entityManager.createNativeQuery("""
                    UPDATE company_trial_windows
                    SET end_date = '2026-10-15', window_days = 45
                    WHERE id = :windowId
                    """).setParameter("windowId", ventanaId).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("R-TRIAL-03 · una fecha de fin inventada, distinta de la calculada, muere en el"
            + " motor")
    void una_fecha_de_fin_inventada_muere_en_el_motor() {
        assertViolates("chk_company_trial_grants_end", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO company_trial_grants (company_id, catalog_item_id, trial_window_id,
                                                      trial_window_end_date, granted_on,
                                                      days_granted, trial_end_date,
                                                      policy_trial_days, policy_trial_outcome,
                                                      source_quote_id, granting_amendment_id,
                                                      consumed_at, outcome, created_date, version)
                    VALUES (:companyId, :itemId, :windowId, '2026-09-30', '2026-09-16', 30,
                            '2026-10-15', 30, 'LIMITED', :quoteId, NULL, NULL, NULL, NOW(), 0)
                    """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                    .setParameter("itemId", nucleo).setParameter("windowId", ventanaId)
                    .setParameter("quoteId", SchemaSeed.QUOTE_ID).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("resolver la prueba escribe su desenlace y la saca del barrido de vencimientos")
    void resolver_la_prueba_escribe_su_desenlace_y_la_saca_del_barrido() {
        CompanyTrialGrant concesion = repository.save(CompanyTrialGrant.grant(ventana(), nucleo,
                INICIO, 14, 14, TrialPolicyOutcome.LIMITED, SchemaSeed.QUOTE_ID, null, CREADA));
        entityManager.flush();

        assertThat(repository.findLiveExpiredOn(LocalDate.of(2026, 9, 20))).hasSize(1);

        repository
                .save(concesion.consume(LocalDateTime.of(2026, 9, 15, 0, 5), TrialOutcome.LIMITED));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findLiveExpiredOn(LocalDate.of(2026, 9, 20))).isEmpty();
        assertThat(repository.findByCompanyIdAndCatalogItemId(SchemaSeed.COMPANY_ID, nucleo)).get()
                .satisfies(leida -> assertThat(leida.getOutcome()).isEqualTo(TrialOutcome.LIMITED));
    }

    @Test
    @DisplayName("las concesiones de una clínica no se ven desde otra")
    void las_concesiones_de_una_clinica_no_se_ven_desde_otra() {
        repository.save(CompanyTrialGrant.grant(ventana(), nucleo, INICIO, 30, 30,
                TrialPolicyOutcome.LIMITED, SchemaSeed.QUOTE_ID, null, CREADA));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID)).hasSize(1);
        assertThat(repository.findAllByCompanyId(SchemaSeed.OTRA_COMPANY_ID)).isEmpty();
        assertThat(repository.existsByCompanyIdAndCatalogItemId(SchemaSeed.OTRA_COMPANY_ID, nucleo))
                .isFalse();
    }
}
