package com.vetsoftware.app.entitlement.infrastructure.persistence;

import static com.vetsoftware.app.testsupport.EngineConstraint.assertViolates;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.entitlement.domain.AccessLevel;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.EntitlementSource;
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
@DisplayName("JpaCompanyEntitlementRepository — permisos efectivos contra MySQL real")
class CompanyEntitlementPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaCompanyEntitlementRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("lista por tenant y el borrado derivado no alcanza a otra empresa")
    void lista_por_tenant_y_borra_solo_sus_derivados() {
        assertThat(repository.findPageByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).content())
                .singleElement().satisfies(entitlement -> {
                    assertThat(entitlement.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                    assertThat(entitlement.getAccessLevel()).isEqualTo(AccessLevel.FULL);
                    assertThat(entitlement.getSubModule().id()).isEqualTo(SchemaSeed.SUB_MODULE_ID);
                });

        assertThat(repository.deleteDerivedByCompanyId(SchemaSeed.COMPANY_ID)).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID)).isEmpty();
        assertThat(repository.findAllByCompanyId(SchemaSeed.OTRA_COMPANY_ID))
                .extracting(CompanyEntitlement::getId)
                .containsExactly(SchemaSeed.OTRO_ENTITLEMENT_ID);
    }

    /**
     * <b>R-ENT-12 contra el motor: el permiso «gratuito con techo» se puede
     * escribir.</b>
     *
     * <p>
     * {@code FREE_LIMITED} es uno de los dos valores que el changeset 246 abrió
     * para que exista la <em>fila sucesora</em>: la que hace que al vencer la
     * prueba el acceso <b>baje</b> en vez de desaparecer. Hasta ahora lo único que
     * lo tocaba era una prueba de traducción del mapper —enum de Java a cadena y
     * vuelta—, y esa pasa igual con {@code chk_company_entitlements_source} mal
     * escrito o sin el valor: el mapper no consulta la base.
     *
     * <p>
     * Aquí la fila entra en MySQL de verdad, y con la forma que tendría en
     * producción: sucesora de la fila de prueba, con su propio {@code valid_from}
     * —{@code uq_company_entitlements} es (empresa, submódulo, inicio de vigencia),
     * así que las dos conviven— y con acceso completo, que es lo que «gratis con
     * techo» significa: usa el módulo, y lo que le frena es el cupo, no el permiso.
     */
    @Test
    @DisplayName("R-ENT-12 · un permiso con origen FREE_LIMITED entra en el motor como fila"
            + " sucesora de la de prueba")
    void un_permiso_gratuito_con_techo_entra_en_el_motor() {
        insertarPermiso("FREE_LIMITED", SchemaSeed.SUBSCRIPTION_ID, "2026-02-01 00:00:00");
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID))
                .filteredOn(permiso -> permiso.getSource() == EntitlementSource.FREE_LIMITED)
                .singleElement().satisfies(sucesora -> {
                    assertThat(sucesora.getSubModule().id()).isEqualTo(SchemaSeed.SUB_MODULE_ID);
                    assertThat(sucesora.getValidFrom())
                            .isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0));
                    assertThat(sucesora.getAccessLevel().allowsRead()).isTrue();
                });
    }

    /**
     * La otra mitad, y la que la prueba de traducción no puede dar: un
     * {@code FREE_LIMITED} sin contrato detrás es un permiso huérfano que el
     * recálculo no sabe revocar — se quedaría concediendo acceso gratis para
     * siempre, sin nada que lo explique. Lo para
     * {@code chk_company_entitlements_origin}, que nombra los cuatro orígenes
     * derivados del contrato.
     */
    @Test
    @DisplayName("un FREE_LIMITED sin contrato detrás muere en chk_company_entitlements_origin")
    void un_permiso_gratuito_con_techo_sin_contrato_muere_en_el_motor() {
        assertViolates("chk_company_entitlements_origin",
                () -> insertarPermiso("FREE_LIMITED", null, "2026-02-01 00:00:00"));
    }

    private void insertarPermiso(String origen, Long contratoId, String desde) {
        entityManager.createNativeQuery("""
                INSERT INTO company_entitlements (company_id, sub_module_id, access_level, source,
                                                  subscription_id, subscription_item_id,
                                                  valid_from, valid_until, recalculated_at,
                                                  created_date)
                VALUES (:companyId, :subModuleId, 'FULL', :origen, :contratoId, NULL, :desde, NULL,
                        NOW(), NOW())
                """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("subModuleId", SchemaSeed.SUB_MODULE_ID)
                .setParameter("origen", origen).setParameter("contratoId", contratoId)
                .setParameter("desde", LocalDateTime.parse(desde.replace(' ', 'T')))
                .executeUpdate();
        entityManager.flush();
    }
}
