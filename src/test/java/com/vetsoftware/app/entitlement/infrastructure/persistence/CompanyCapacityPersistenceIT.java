package com.vetsoftware.app.entitlement.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyCapacityRepository — contadores contratados contra MySQL real")
class CompanyCapacityPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaCompanyCapacityRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        seedCapacity(977L, CapacityUnit.BRANCH);
        seedCapacity(978L, CapacityUnit.TERMINAL);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CapacityUnit.class, names = {"USER", "BRANCH", "TERMINAL"})
    @DisplayName("addUsage incrementa atómicamente USER, BRANCH y TERMINAL")
    void add_usage_incrementa_el_contador_correcto(CapacityUnit unit) {
        int filas = repository.addUsage(SchemaSeed.COMPANY_ID, unit, 2);
        entityManager.flush();
        entityManager.clear();

        assertThat(filas).isEqualTo(1);
        assertThat(repository.findByCompanyIdAndUnit(SchemaSeed.COMPANY_ID, unit)).get()
                .satisfies(capacidad -> {
                    assertThat(capacidad.getLimitQuantity()).isEqualTo(2);
                    assertThat(capacidad.getUsedQuantity()).isEqualTo(2);
                    assertThat(capacidad.getSubscriptionId()).isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
                });
        assertThat(repository.addUsage(SchemaSeed.OTRA_COMPANY_ID, unit, 1)).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CapacityUnit.class, names = {"USER", "BRANCH", "TERMINAL"})
    @DisplayName("addUsage no rebasa el límite ni permite dejar el contador negativo")
    void add_usage_respeta_limite_y_piso_de_cero(CapacityUnit unit) {
        assertThat(repository.addUsage(SchemaSeed.COMPANY_ID, unit, 2)).isEqualTo(1);
        assertThat(repository.addUsage(SchemaSeed.COMPANY_ID, unit, 1)).isZero();
        assertThat(repository.addUsage(SchemaSeed.COMPANY_ID, unit, -3)).isZero();
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByCompanyIdAndUnit(SchemaSeed.COMPANY_ID, unit)).get()
                .extracting(capacity -> capacity.getUsedQuantity()).isEqualTo(2);
    }

    private void seedCapacity(Long id, CapacityUnit unit) {
        entityManager.createNativeQuery("""
                INSERT INTO company_capacities (id, company_id, capacity_unit, limit_quantity,
                                                used_quantity, subscription_id,
                                                recalculated_at, created_date)
                VALUES (:id, :companyId, :unit, 2, 0, :subscriptionId, NOW(), NOW())
                ON DUPLICATE KEY UPDATE used_quantity = 0, limit_quantity = 2
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("unit", unit.name())
                .setParameter("subscriptionId", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();
        entityManager.flush();
    }
}
