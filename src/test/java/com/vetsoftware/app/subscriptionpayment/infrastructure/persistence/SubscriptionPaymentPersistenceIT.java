package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSubscriptionPaymentRepository — pagos recibidos contra MySQL real")
class SubscriptionPaymentPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSubscriptionPaymentRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("guarda un pago pendiente y lo deduplica por llave del cliente")
    void guarda_pago_y_lo_encuentra_por_llave() {
        LocalDateTime at = LocalDateTime.of(2026, 8, 23, 10, 0);
        SubscriptionPayment saved = repository.save(
                SubscriptionPayment.register(SchemaSeed.COMPANY_ID, new BigDecimal("250000.00"),
                        "COP", PaymentMethod.TRANSFER, null, null, at, "payment-request-1", at));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByCompanyIdAndClientRequestId(SchemaSeed.COMPANY_ID,
                "payment-request-1")).get().satisfies(payment -> {
                    assertThat(payment.getId()).isEqualTo(saved.getId());
                    assertThat(payment.getStatus()).isEqualTo(SubscriptionPaymentStatus.PENDING);
                    assertThat(payment.getAmount()).isEqualByComparingTo("250000.00");
                });
        assertThat(repository.findByIdAndCompanyId(saved.getId(), SchemaSeed.OTRA_COMPANY_ID))
                .isEmpty();
    }
}
