package com.vetsoftware.app.entitlement.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaEntitlementEffectivePermissionResolver — arbitraje contractual contra MySQL")
class JpaEntitlementEffectivePermissionResolverIT extends AbstractDataJpaTest {

    private static final Long READ_ONLY_SUB_MODULE = 982L;
    private static final Long NONE_SUB_MODULE = 983L;
    private static final Long EXPIRED_SUB_MODULE = 984L;
    private static final Long ABSENT_SUB_MODULE = 985L;
    private static final Long FOREIGN_SUB_MODULE = 986L;

    @Autowired
    private JpaEntitlementEffectivePermissionResolver resolver;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        subModule(READ_ONLY_SUB_MODULE, "READ_ONLY_TEST");
        subModule(NONE_SUB_MODULE, "NONE_TEST");
        subModule(EXPIRED_SUB_MODULE, "EXPIRED_TEST");
        subModule(ABSENT_SUB_MODULE, "ABSENT_TEST");
        subModule(FOREIGN_SUB_MODULE, "FOREIGN_TEST");

        permission(1101L, SchemaSeed.COMPANY_ID, SchemaSeed.SUB_MODULE_ID, "animal.read");
        permission(1102L, SchemaSeed.COMPANY_ID, SchemaSeed.SUB_MODULE_ID, "animal.update");
        permission(1103L, SchemaSeed.COMPANY_ID, READ_ONLY_SUB_MODULE, "billing.read");
        permission(1104L, SchemaSeed.COMPANY_ID, READ_ONLY_SUB_MODULE, "billing.update");
        permission(1105L, SchemaSeed.COMPANY_ID, NONE_SUB_MODULE, "reports.read");
        permission(1106L, SchemaSeed.COMPANY_ID, EXPIRED_SUB_MODULE, "archive.read");
        permission(1107L, SchemaSeed.COMPANY_ID, ABSENT_SUB_MODULE, "absent.read");
        permission(1108L, SchemaSeed.COMPANY_ID, FOREIGN_SUB_MODULE, "foreign.read");

        entitlement(1201L, SchemaSeed.COMPANY_ID, READ_ONLY_SUB_MODULE, "READ_ONLY",
                "2026-01-01 00:00:00", null);
        entitlement(1202L, SchemaSeed.COMPANY_ID, NONE_SUB_MODULE, "NONE", "2026-01-01 00:00:00",
                null);
        entitlement(1203L, SchemaSeed.COMPANY_ID, EXPIRED_SUB_MODULE, "FULL", "2026-01-01 00:00:00",
                "2026-01-02 00:00:00");
        entitlement(1204L, SchemaSeed.OTRA_COMPANY_ID, FOREIGN_SUB_MODULE, "FULL",
                "2026-01-01 00:00:00", null);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("FULL conserva lectura y mutación que ya estaban en el rol del empleado")
    void full_conserva_lectura_y_mutacion() {
        assertThat(
                resolver.resolveFor(SchemaSeed.COMPANY_ID, Set.of("animal.read", "animal.update")))
                .containsExactlyInAnyOrder("animal.read", "animal.update");
    }

    @Test
    @DisplayName("READ_ONLY conserva .read y elimina las autoridades de mutación")
    void read_only_conserva_read_y_elimina_mutaciones() {
        assertThat(resolver.resolveFor(SchemaSeed.COMPANY_ID,
                Set.of("billing.read", "billing.update"))).containsExactly("billing.read");
    }

    @Test
    @DisplayName("NONE, ausencia, otro tenant y una ventana vencida no conceden acceso")
    void estados_sin_concesion_no_dan_acceso() {
        assertThat(resolver.resolveFor(SchemaSeed.COMPANY_ID,
                Set.of("reports.read", "absent.read", "foreign.read", "archive.read"))).isEmpty();
    }

    @Test
    @DisplayName("empresa o permisos base ausentes terminan sin consulta útil ni concesiones")
    void entradas_ausentes_terminan_sin_concesiones() {
        assertThat(resolver.resolveFor(null, Set.of("animal.read"))).isEmpty();
        assertThat(resolver.resolveFor(SchemaSeed.COMPANY_ID, Set.of())).isEmpty();
        assertThat(resolver.resolveFor(SchemaSeed.COMPANY_ID, null)).isEmpty();
    }

    private void subModule(Long id, String code) {
        entityManager.createNativeQuery("""
                INSERT INTO sub_modules (id, name, code, module_id, created_date, enabled, version,
                                         is_sellable, read_only_capable)
                VALUES (:id, :code, :code, :moduleId, NOW(), true, 0, true, true)
                """).setParameter("id", id).setParameter("code", code)
                .setParameter("moduleId", SchemaSeed.MODULE_ID).executeUpdate();
    }

    private void permission(Long id, Long companyId, Long subModuleId, String code) {
        entityManager.createNativeQuery("""
                INSERT INTO permissions (id, name, code, company_id, sub_module_id, created_date,
                                         enabled)
                VALUES (:id, :code, :code, :companyId, :subModuleId, NOW(), true)
                """).setParameter("id", id).setParameter("code", code)
                .setParameter("companyId", companyId).setParameter("subModuleId", subModuleId)
                .executeUpdate();
    }

    private void entitlement(Long id, Long companyId, Long subModuleId, String accessLevel,
            String validFrom, String validUntil) {
        entityManager.createNativeQuery("""
                INSERT INTO company_entitlements
                    (id, company_id, sub_module_id, access_level, source, subscription_id,
                     subscription_item_id, valid_from, valid_until, recalculated_at, created_date)
                VALUES (:id, :companyId, :subModuleId, :accessLevel, 'MANUAL_GRANT', NULL, NULL,
                        :validFrom, :validUntil, NOW(), NOW())
                """).setParameter("id", id).setParameter("companyId", companyId)
                .setParameter("subModuleId", subModuleId).setParameter("accessLevel", accessLevel)
                .setParameter("validFrom", validFrom).setParameter("validUntil", validUntil)
                .executeUpdate();
    }
}
