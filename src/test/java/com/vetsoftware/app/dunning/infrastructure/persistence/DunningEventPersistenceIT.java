package com.vetsoftware.app.dunning.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.dunning.domain.DunningEvent;
import com.vetsoftware.app.dunning.domain.DunningEventType;
import com.vetsoftware.app.dunning.domain.SubscriptionRef;
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
@DisplayName("JpaDunningEventRepository — expediente append-only contra MySQL real")
class DunningEventPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaDunningEventRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("anota el inicio de gracia y lo lista solo para su empresa")
    void anota_inicio_de_gracia_y_acota_por_empresa() {
        LocalDateTime at = LocalDateTime.of(2026, 2, 6, 9, 0);
        DunningEvent saved = repository.save(DunningEvent.record(SchemaSeed.COMPANY_ID,
                new SubscriptionRef(SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.COMPANY_ID,
                        "SUS-TEST-000900", "ACTIVE"),
                null, DunningEventType.GRACE_STARTED, 1, null, "Inicia gracia", at, at));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllBySubscriptionIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ID,
                SchemaSeed.COMPANY_ID, 0, 20).content()).singleElement().satisfies(event -> {
                    assertThat(event.getId()).isEqualTo(saved.getId());
                    assertThat(event.getEventType()).isEqualTo(DunningEventType.GRACE_STARTED);
                    assertThat(event.getDaysOverdue()).isEqualTo(1);
                });
        assertThat(repository.findByIdAndCompanyId(saved.getId(), SchemaSeed.OTRA_COMPANY_ID))
                .isEmpty();
    }
}
