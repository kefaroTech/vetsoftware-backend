package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaBillingDocumentRepository — cabecera y desglose fiscal contra MySQL real")
class BillingDocumentPersistenceIT extends AbstractDataJpaTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-01T10:00:00Z"),
            ZoneOffset.UTC);
    private static final ServicePeriod PERIOD = new ServicePeriod(LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28));

    @Autowired
    private JpaBillingDocumentRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("guarda el documento y su impuesto como un solo agregado")
    void guarda_documento_y_desglose_como_agregado() {
        SubscriptionCharge charge = SubscriptionCharge.create(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, ChargeType.RECURRING,
                "Cuota febrero", PERIOD, BigDecimal.ONE, new BigDecimal("100000.00"),
                new BigDecimal("100000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, null,
                null, CLOCK);
        TaxBreakdown breakdown = TaxBreakdown.of(List.of(charge), DocumentKind.INVOICE,
                SchemaSeed.COMPANY_ID, java.time.LocalDateTime.now(CLOCK));
        SubscriptionBillingDocument saved = repository
                .save(SubscriptionBillingDocument.issue(new DocumentNumber("DCT", 1),
                        SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, DocumentKind.INVOICE,
                        BillingReason.RECURRING_CYCLE, PERIOD, breakdown, null, CLOCK));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByIdAndCompanyId(saved.getId(), SchemaSeed.COMPANY_ID)).get()
                .satisfies(document -> {
                    assertThat(document.getDocumentNumber()).isEqualTo("DCT-000001");
                    assertThat(document.getTotalAmount()).isEqualByComparingTo("119000.00");
                    assertThat(document.getTaxes()).singleElement().satisfies(tax -> {
                        assertThat(tax.taxRate()).isEqualByComparingTo("19.00");
                        assertThat(tax.taxAmount()).isEqualByComparingTo("19000.00");
                    });
                });
        assertThat(repository.existsRecurringCycle(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, PERIOD.start(), PERIOD.end())).isTrue();
    }
}
