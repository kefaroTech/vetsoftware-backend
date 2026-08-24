package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
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
@DisplayName("JpaSubscriptionStatusHistoryRepository — bitácora contra MySQL real")
class SubscriptionStatusHistoryPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSubscriptionStatusHistoryRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("anexa la transición y la lista solo dentro del tenant")
    void anexa_transicion_y_lista_dentro_del_tenant() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 2, 5, 8, 30);
        SubscriptionStatusChange saved = repository.append(SubscriptionStatusChange.record(
                SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, SubscriptionStatus.ACTIVE,
                SubscriptionStatus.PAST_DUE, "Factura vencida", "billing-job", occurredAt));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllBySubscriptionIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ID,
                SchemaSeed.COMPANY_ID, 0, 20).content()).singleElement().satisfies(change -> {
                    assertThat(change.getId()).isEqualTo(saved.getId());
                    assertThat(change.getToStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
                    assertThat(change.getOccurredAt()).isEqualTo(occurredAt);
                });
        assertThat(repository.findAllBySubscriptionIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ID,
                SchemaSeed.OTRA_COMPANY_ID, 0, 20).content()).isEmpty();
    }
}
