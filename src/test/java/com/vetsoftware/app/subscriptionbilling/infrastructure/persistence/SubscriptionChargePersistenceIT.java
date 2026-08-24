package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSubscriptionChargeRepository — devengos contra MySQL real")
class SubscriptionChargePersistenceIT extends AbstractDataJpaTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-01T10:00:00Z"),
            ZoneOffset.UTC);
    private static final ServicePeriod PERIOD = new ServicePeriod(LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28));

    @Autowired
    private JpaSubscriptionChargeRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("guarda el devengo pendiente con importe fiscal y periodo exactos")
    void guarda_devengo_pendiente_con_periodo_exacto() {
        SubscriptionCharge saved = repository.save(SubscriptionCharge.create(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, ChargeType.RECURRING,
                "Cuota febrero", PERIOD, BigDecimal.ONE, new BigDecimal("100000.00"),
                new BigDecimal("100000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, null,
                null, CLOCK));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findPendingByCompanyIdAndSubscription(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, PERIOD.start(), PERIOD.end())).singleElement()
                .satisfies(charge -> {
                    assertThat(charge.getId()).isEqualTo(saved.getId());
                    assertThat(charge.getSubtotalAmount()).isEqualByComparingTo("100000.00");
                    assertThat(charge.getServicePeriod()).isEqualTo(PERIOD);
                });
        assertThat(repository.findByIdAndCompanyId(saved.getId(), SchemaSeed.OTRA_COMPANY_ID))
                .isEmpty();
    }
}
