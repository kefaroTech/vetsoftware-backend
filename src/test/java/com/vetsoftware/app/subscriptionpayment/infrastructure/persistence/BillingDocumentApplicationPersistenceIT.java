package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentNumber;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.TaxBreakdown;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.JpaBillingDocumentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentRef;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaBillingDocumentApplicationRepository — imputaciones contra MySQL real")
class BillingDocumentApplicationPersistenceIT extends AbstractDataJpaTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-01T10:00:00Z"),
            ZoneOffset.UTC);
    private static final ServicePeriod PERIOD = new ServicePeriod(LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28));

    @Autowired
    private JpaBillingDocumentApplicationRepository repository;
    @Autowired
    private JpaBillingDocumentRepository documents;
    @Autowired
    private JpaSubscriptionPaymentRepository payments;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("aplica un pago al documento y la suma neta queda en la base")
    void aplica_pago_y_calcula_suma_neta() {
        SubscriptionBillingDocument document = documents.save(document());
        LocalDateTime at = LocalDateTime.now(CLOCK);
        SubscriptionPayment payment = payments.save(SubscriptionPayment.register(
                SchemaSeed.COMPANY_ID, new BigDecimal("50000.00"), "COP", PaymentMethod.TRANSFER,
                null, null, at, "payment-for-application", at));
        BillingDocumentRef target = new BillingDocumentRef(document.getId(), SchemaSeed.COMPANY_ID,
                document.getDocumentNumber(), "INVOICE", document.getTotalAmount(),
                document.getBalanceAmount());

        BillingDocumentApplication saved = repository
                .save(BillingDocumentApplication.fromPayment(SchemaSeed.COMPANY_ID, target,
                        payment.getId(), new BigDecimal("50000.00"), "application-request-1", at));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.sumAppliedFromPayment(payment.getId(), SchemaSeed.COMPANY_ID))
                .isEqualByComparingTo("50000.00");
        assertThat(repository.findAllByTargetDocumentIdAndCompanyId(document.getId(),
                SchemaSeed.COMPANY_ID, 0, 20).content()).singleElement()
                .extracting(BillingDocumentApplication::getId).isEqualTo(saved.getId());
        assertThat(repository.findByIdAndCompanyId(saved.getId(), SchemaSeed.OTRA_COMPANY_ID))
                .isEmpty();
    }

    private static SubscriptionBillingDocument document() {
        SubscriptionCharge charge = SubscriptionCharge.create(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, ChargeType.RECURRING,
                "Cuota febrero", PERIOD, BigDecimal.ONE, new BigDecimal("100000.00"),
                new BigDecimal("100000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, null,
                null, CLOCK);
        TaxBreakdown breakdown = TaxBreakdown.of(List.of(charge), DocumentKind.INVOICE,
                SchemaSeed.COMPANY_ID, LocalDateTime.now(CLOCK));
        return SubscriptionBillingDocument.issue(new DocumentNumber("APP", 1),
                SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, DocumentKind.INVOICE,
                BillingReason.ONE_TIME, PERIOD, breakdown, null, CLOCK);
    }
}
