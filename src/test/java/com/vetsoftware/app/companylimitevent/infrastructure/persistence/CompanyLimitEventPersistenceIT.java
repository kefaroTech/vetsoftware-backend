package com.vetsoftware.app.companylimitevent.infrastructure.persistence;

import static com.vetsoftware.app.testsupport.EngineConstraint.assertViolates;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companylimitevent.domain.CompanyLimitEvent;
import com.vetsoftware.app.companylimitevent.domain.EventActor;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyLimitEventRepository — la bitácora de cupo contra MySQL real")
class CompanyLimitEventPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime EN_MARZO = LocalDateTime.of(2026, 3, 14, 10, 30);
    private static final LocalDateTime DESDE = LocalDateTime.of(2026, 3, 1, 0, 0);
    private static final LocalDateTime HASTA = LocalDateTime.of(2026, 3, 31, 23, 59);

    @Autowired
    private JpaCompanyLimitEventRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Resuelto, no sembrado: los ocho ejes llegan poblados por el changeset 313.
     */
    private Long ejeAnimal;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        entityManager.flush();
        ejeAnimal = SchemaSeed.limitDimensionId(entityManager, "ANIMAL");
    }

    @Test
    @DisplayName("R-LIMIT-18 · el portazo deja su fila con los tres números del momento y el"
            + " empleado que lo intentó")
    void el_portazo_deja_su_fila_con_los_tres_numeros_del_momento() {
        repository.append(CompanyLimitEvent.record(SchemaSeed.COMPANY_ID, ejeAnimal,
                LimitEventType.LIMIT_BLOCKED, 100, 100, 1, LimitSource.CATALOG_DEFAULT, null,
                EventActor.employee(SchemaSeed.EMPLOYEE_ID), null, null, EN_MARZO));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByCompanyIdBetween(SchemaSeed.COMPANY_ID, DESDE, HASTA))
                .singleElement().satisfies(hecho -> {
                    assertThat(hecho.getEventType()).isEqualTo(LimitEventType.LIMIT_BLOCKED);
                    assertThat(hecho.getLimitQuantity()).isEqualTo(100);
                    assertThat(hecho.getUsedQuantity()).isEqualTo(100);
                    assertThat(hecho.getRequestedDelta()).isEqualTo(1);
                    assertThat(hecho.getActor().employeeId()).isEqualTo(SchemaSeed.EMPLOYEE_ID);
                });
    }

    @Test
    @DisplayName("R-LIMIT-19 · la corrección de plataforma guarda su motivo y la firma de quien la"
            + " hizo")
    void la_correccion_de_plataforma_guarda_motivo_y_firma() {
        repository.append(CompanyLimitEvent.record(SchemaSeed.COMPANY_ID, ejeAnimal,
                LimitEventType.USAGE_ADJUSTED, 100, 600, -500, LimitSource.NONE, null,
                EventActor.systemUser(SchemaSeed.SYSTEM_USER_ID), "MIGRATION",
                "Migración duplicada del 14/03, ticket SOP-118", EN_MARZO));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByCompanyIdBetween(SchemaSeed.COMPANY_ID, DESDE, HASTA))
                .singleElement().satisfies(hecho -> {
                    assertThat(hecho.getActor().systemUserId())
                            .isEqualTo(SchemaSeed.SYSTEM_USER_ID);
                    assertThat(hecho.getReasonCode()).isEqualTo("MIGRATION");
                    assertThat(hecho.getReason()).contains("SOP-118");
                });
    }

    @Test
    @DisplayName("una corrección sin motivo escrito muere en el motor")
    void una_correccion_sin_motivo_escrito_muere_en_el_motor() {
        assertViolates("chk_company_limit_events_reason", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO company_limit_events (company_id, limit_dimension_id, event_type,
                                                      limit_quantity, used_quantity,
                                                      requested_delta, limit_source, override_id,
                                                      actor_employee_id, actor_system_user_id,
                                                      actor_is_process, reason_code, reason,
                                                      occurred_at, created_date)
                    VALUES (:companyId, :dimensionId, 'USAGE_ADJUSTED', 100, 600, -500, 'NONE',
                            NULL, NULL, :userId, false, NULL, NULL, NOW(), NOW())
                    """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                    .setParameter("dimensionId", ejeAnimal)
                    .setParameter("userId", SchemaSeed.SYSTEM_USER_ID).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("un hecho con dos actores a la vez muere en el motor")
    void un_hecho_con_dos_actores_muere_en_el_motor() {
        assertViolates("chk_company_limit_events_actor", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO company_limit_events (company_id, limit_dimension_id, event_type,
                                                      limit_quantity, used_quantity,
                                                      requested_delta, limit_source, override_id,
                                                      actor_employee_id, actor_system_user_id,
                                                      actor_is_process, reason_code, reason,
                                                      occurred_at, created_date)
                    VALUES (:companyId, :dimensionId, 'LIMIT_BLOCKED', 100, 100, 1,
                            'CATALOG_DEFAULT', NULL, :employeeId, :userId, false, NULL, NULL,
                            NOW(), NOW())
                    """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                    .setParameter("dimensionId", ejeAnimal)
                    .setParameter("employeeId", SchemaSeed.EMPLOYEE_ID)
                    .setParameter("userId", SchemaSeed.SYSTEM_USER_ID).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("la bitácora de una clínica no se ve desde otra")
    void la_bitacora_de_una_clinica_no_se_ve_desde_otra() {
        repository.append(CompanyLimitEvent.record(SchemaSeed.COMPANY_ID, ejeAnimal,
                LimitEventType.THRESHOLD_WARNED, 100, 80, 1, LimitSource.SUBSCRIPTION, null,
                EventActor.automatedProcess(), null, null, EN_MARZO));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByCompanyIdBetween(SchemaSeed.COMPANY_ID, DESDE, HASTA))
                .hasSize(1);
        assertThat(repository.findAllByCompanyIdBetween(SchemaSeed.OTRA_COMPANY_ID, DESDE, HASTA))
                .isEmpty();
    }
}
