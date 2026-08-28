package com.vetsoftware.app.companyentitlementsnapshot.infrastructure.persistence;

import static com.vetsoftware.app.testsupport.EngineConstraint.assertViolates;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companyentitlementsnapshot.domain.CompanyEntitlementSnapshot;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotActor;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotTriggerReason;
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
@DisplayName("JpaCompanyEntitlementSnapshotRepository — la foto de permisos contra MySQL real")
class CompanyEntitlementSnapshotPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime TRES_DE_MARZO = LocalDateTime.of(2026, 3, 3, 11, 0);
    private static final LocalDateTime DIEZ_DE_MARZO = LocalDateTime.of(2026, 3, 10, 11, 0);
    private static final String PAYLOAD = "{\"entitlements\":[{\"subModule\":\"AGENDA\","
            + "\"accessLevel\":\"FULL\"}]}";

    @Autowired
    private JpaCompanyEntitlementSnapshotRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("guarda el documento JSON y lo devuelve intacto: es la prueba de que el mapeo de"
            + " la columna JSON valida contra el esquema real")
    void guarda_el_documento_json_y_lo_devuelve_intacto() {
        repository.append(CompanyEntitlementSnapshot.take(SchemaSeed.COMPANY_ID, TRES_DE_MARZO,
                SnapshotActor.automatedProcess(), SnapshotTriggerReason.TRIAL_EXPIRED, null,
                PAYLOAD, 1));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findLatestAsOf(SchemaSeed.COMPANY_ID, DIEZ_DE_MARZO)).get()
                .satisfies(foto -> {
                    assertThat(foto.getPayload()).contains("AGENDA");
                    assertThat(foto.getPayloadFormatVersion()).isEqualTo(1);
                    assertThat(foto.getTriggerReason())
                            .isEqualTo(SnapshotTriggerReason.TRIAL_EXPIRED);
                });
    }

    @Test
    @DisplayName("«qué veía el 3 de marzo» devuelve la foto de ese día, no la posterior")
    void que_veia_el_3_de_marzo_devuelve_la_foto_de_ese_dia() {
        repository.append(CompanyEntitlementSnapshot.take(SchemaSeed.COMPANY_ID, TRES_DE_MARZO,
                SnapshotActor.automatedProcess(), SnapshotTriggerReason.TRIAL_EXPIRED, null,
                "{\"marca\":\"tres\"}", 1));
        repository.append(CompanyEntitlementSnapshot.take(SchemaSeed.COMPANY_ID, DIEZ_DE_MARZO,
                SnapshotActor.automatedProcess(), SnapshotTriggerReason.DUNNING, null,
                "{\"marca\":\"diez\"}", 1));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findLatestAsOf(SchemaSeed.COMPANY_ID,
                LocalDateTime.of(2026, 3, 5, 0, 0))).get()
                .satisfies(foto -> assertThat(foto.getPayload()).contains("tres"));
        assertThat(repository.findLatestAsOf(SchemaSeed.COMPANY_ID,
                LocalDateTime.of(2026, 3, 20, 0, 0))).get()
                .satisfies(foto -> assertThat(foto.getPayload()).contains("diez"));
    }

    @Test
    @DisplayName("antes de la primera foto no hay respuesta que inventar")
    void antes_de_la_primera_foto_no_hay_respuesta() {
        repository.append(CompanyEntitlementSnapshot.take(SchemaSeed.COMPANY_ID, TRES_DE_MARZO,
                SnapshotActor.automatedProcess(), SnapshotTriggerReason.REPAIR, null, PAYLOAD, 1));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findLatestAsOf(SchemaSeed.COMPANY_ID,
                LocalDateTime.of(2026, 1, 1, 0, 0))).isEmpty();
    }

    @Test
    @DisplayName("una foto de otrosí sin el otrosí muere en el motor")
    void una_foto_de_otrosi_sin_el_otrosi_muere_en_el_motor() {
        assertViolates("chk_company_entitlement_snapshots_amendment", () -> {
            entityManager.createNativeQuery("""
                    INSERT INTO company_entitlement_snapshots (company_id, recalculated_at,
                                                               actor_employee_id,
                                                               actor_system_user_id,
                                                               actor_is_process, trigger_reason,
                                                               amendment_id, payload,
                                                               payload_format_version,
                                                               created_date)
                    VALUES (:companyId, NOW(), NULL, NULL, true, 'CONTRACT_AMENDMENT', NULL,
                            '{}', 1, NOW())
                    """).setParameter("companyId", SchemaSeed.COMPANY_ID).executeUpdate();
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("las fotos de una clínica no se ven desde otra")
    void las_fotos_de_una_clinica_no_se_ven_desde_otra() {
        repository.append(CompanyEntitlementSnapshot.take(SchemaSeed.COMPANY_ID, TRES_DE_MARZO,
                SnapshotActor.automatedProcess(), SnapshotTriggerReason.MANUAL, null, PAYLOAD, 1));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByCompanyIdBetween(SchemaSeed.COMPANY_ID,
                LocalDateTime.of(2026, 3, 1, 0, 0), LocalDateTime.of(2026, 3, 31, 23, 59)))
                .hasSize(1);
        assertThat(repository.findAllByCompanyIdBetween(SchemaSeed.OTRA_COMPANY_ID,
                LocalDateTime.of(2026, 3, 1, 0, 0), LocalDateTime.of(2026, 3, 31, 23, 59)))
                .isEmpty();
    }
}
