package com.vetsoftware.app.companytrialwindow.infrastructure.persistence;

import static com.vetsoftware.app.testsupport.EngineConstraint.assertViolates;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindow;
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
@DisplayName("JpaCompanyTrialWindowRepository — el reloj de la empresa contra MySQL real")
class CompanyTrialWindowPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 9, 1, 8, 0);

    @Autowired
    private JpaCompanyTrialWindowRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    private CompanyTrialWindow abrirVentanaDe30Dias(Long companyId, Long quoteId) {
        return repository.save(CompanyTrialWindow.open(companyId, INICIO, 30, quoteId, CREADA));
    }

    @Test
    @DisplayName("R-TRIAL-02 · una ventana de 30 días abierta el 1 de septiembre se guarda con fin"
            + " el 30, y la restricción del motor lo confirma")
    void una_ventana_de_30_dias_se_guarda_con_fin_el_30_de_septiembre() {
        CompanyTrialWindow guardada = abrirVentanaDe30Dias(SchemaSeed.COMPANY_ID,
                SchemaSeed.QUOTE_ID);
        entityManager.flush();
        entityManager.clear();

        assertThat(guardada.getId()).isNotNull();
        assertThat(repository.findOpenByCompanyId(SchemaSeed.COMPANY_ID)).get().satisfies(leida -> {
            assertThat(leida.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
            assertThat(leida.isOpen()).isTrue();
        });
    }

    @Test
    @DisplayName("R-TRIAL-02 · una fila con el fin mal calculado muere en el motor aunque el"
            + " dominio no la vea pasar")
    void una_fila_con_el_fin_mal_calculado_muere_en_el_motor() {
        assertViolates("chk_company_trial_windows_end", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO company_trial_windows (company_id, start_date, end_date,
                                                       window_days, source_quote_id, closed_at,
                                                       created_date, version)
                    VALUES (:companyId, '2026-09-01', '2026-10-01', 30, :quoteId, NULL, NOW(), 0)
                    """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                    .setParameter("quoteId", SchemaSeed.QUOTE_ID).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("R-TRIAL-01 · abrir una segunda ventana con la primera sin cerrar falla en el"
            + " motor")
    void abrir_una_segunda_ventana_con_la_primera_sin_closed_at_falla_en_el_motor() {
        abrirVentanaDe30Dias(SchemaSeed.COMPANY_ID, SchemaSeed.QUOTE_ID);
        entityManager.flush();

        assertViolates("uq_company_trial_windows_open", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO company_trial_windows (company_id, start_date, end_date,
                                                       window_days, source_quote_id, closed_at,
                                                       created_date, version)
                    VALUES (:companyId, '2026-10-01', '2026-10-30', 30, :quoteId, NULL, NOW(), 0)
                    """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                    .setParameter("quoteId", SchemaSeed.QUOTE_ID).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("R-TRIAL-01 · cerrar la primera deja abrir otra: es «una abierta», no «una en la"
            + " vida»")
    void abrir_una_ventana_nueva_despues_de_cerrar_la_anterior_funciona() {
        CompanyTrialWindow primera = abrirVentanaDe30Dias(SchemaSeed.COMPANY_ID,
                SchemaSeed.QUOTE_ID);
        repository.save(primera.close(LocalDateTime.of(2026, 9, 30, 23, 59)));
        entityManager.flush();

        CompanyTrialWindow segunda = repository
                .save(CompanyTrialWindow.open(SchemaSeed.COMPANY_ID, LocalDate.of(2028, 5, 1), 30,
                        SchemaSeed.QUOTE_ID, LocalDateTime.of(2028, 5, 1, 8, 0)));
        entityManager.flush();
        entityManager.clear();

        assertThat(segunda.getId()).isNotNull();
        assertThat(repository.findOpenByCompanyId(SchemaSeed.COMPANY_ID)).get().satisfies(
                viva -> assertThat(viva.getStartDate()).isEqualTo(LocalDate.of(2028, 5, 1)));
    }

    @Test
    @DisplayName("la ventana de una clínica no se ve desde otra")
    void la_ventana_de_una_clinica_no_se_ve_desde_otra() {
        abrirVentanaDe30Dias(SchemaSeed.COMPANY_ID, SchemaSeed.QUOTE_ID);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findOpenByCompanyId(SchemaSeed.OTRA_COMPANY_ID)).isEmpty();
        assertThat(repository.existsOpenByCompanyId(SchemaSeed.OTRA_COMPANY_ID)).isFalse();
    }
}
