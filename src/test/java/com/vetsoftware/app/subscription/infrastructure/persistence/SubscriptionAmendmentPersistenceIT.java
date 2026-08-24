package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscription.domain.AmendmentType;
import com.vetsoftware.app.subscription.domain.SubscriptionAmendment;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSubscriptionAmendmentRepository — otrosí append-only contra MySQL real")
class SubscriptionAmendmentPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSubscriptionAmendmentRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("guarda actor, importes firmados y llave idempotente")
    void guarda_actor_importes_y_llave_idempotente() {
        SubscriptionAmendment saved = repository
                .save(SubscriptionAmendment.issue(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                        "OT-2026-0001", AmendmentType.ADD_ITEM, LocalDate.of(2026, 2, 1),
                        "Ampliación", SchemaSeed.EMPLOYEE_ID, null, new BigDecimal("25000.00"),
                        new BigDecimal("50000.00"), null, "amendment-request-1"));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByClientRequestIdAndCompanyId("amendment-request-1",
                SchemaSeed.COMPANY_ID)).get().satisfies(amendment -> {
                    assertThat(amendment.getId()).isEqualTo(saved.getId());
                    assertThat(amendment.getRequestedByEmployeeId())
                            .isEqualTo(SchemaSeed.EMPLOYEE_ID);
                    assertThat(amendment.getProrationAmount()).isEqualByComparingTo("25000.00");
                });
        assertThat(repository.findByIdAndCompanyId(saved.getId(), SchemaSeed.OTRA_COMPANY_ID))
                .isEmpty();
    }
}
