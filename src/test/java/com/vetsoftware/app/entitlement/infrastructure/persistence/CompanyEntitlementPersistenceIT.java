package com.vetsoftware.app.entitlement.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.entitlement.domain.AccessLevel;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
}
