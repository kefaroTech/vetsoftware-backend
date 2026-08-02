package com.vetsoftware.app.infrastructure.observability.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.inventory.application.port.out.InventoryMetrics;
import com.vetsoftware.app.inventory.domain.StockMovementType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MicrometerBusinessMetricsTest {

    private PrometheusMeterRegistry registry;
    private MicrometerBusinessMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().meterFilter(new BusinessMetricCardinalityFilter());
        metrics = new MicrometerBusinessMetrics(registry, new AfterCommitMetricRecorder());
    }

    @Test
    void exportsStablePrometheusNamesAndOnlyBoundedTags() {
        metrics.completed(SalesMetrics.Channel.POS, ElectronicDocumentType.DOC_EQUIV_POS,
                new BigDecimal("125000"), 3);
        metrics.finished(DianStatus.VALIDADO, BillingMetrics.Origin.INITIAL,
                ElectronicDocumentType.DOC_EQUIV_POS, Duration.ofMillis(850));
        metrics.movement(StockMovementType.SALE, InventoryMetrics.Result.SUCCESS, 2);
        metrics.transitioned(AppointmentStatus.CONFIRMED, AppointmentMetrics.Channel.STAFF);
        metrics.opened();
        metrics.closed(List.of(new BigDecimal("-10000"), BigDecimal.ZERO));

        String scrape = registry.scrape();

        assertThat(scrape).contains("vetsoftware_business_sales_operations_total")
                .contains("vetsoftware_business_sales_amount_cop_sum")
                .contains("vetsoftware_business_sales_lines_sum")
                .contains("vetsoftware_business_dian_transmissions_total")
                .contains("vetsoftware_business_dian_transmission_duration_seconds")
                .contains("vetsoftware_business_inventory_movements_total")
                .contains("vetsoftware_business_inventory_units_sum")
                .contains("vetsoftware_business_appointments_transitions_total")
                .contains("vetsoftware_business_cash_sessions_total")
                .contains("vetsoftware_business_cash_closing_difference_cop_sum")
                .contains("document_type=\"doc_equiv_pos\"").doesNotContain("companyId")
                .doesNotContain("productId").doesNotContain("traceId");
    }

    @Test
    void deniesUnknownBusinessTagsAndValues() {
        Counter.builder(BusinessMetricNames.PREFIX + "unsafe").tag("companyId", "123")
                .register(registry).increment();
        Counter.builder(BusinessMetricNames.PREFIX + "unsafe.result")
                .tag("result", "customer-provided-value").register(registry).increment();

        assertThat(registry.find(BusinessMetricNames.PREFIX + "unsafe").counter()).isNull();
        assertThat(registry.find(BusinessMetricNames.PREFIX + "unsafe.result").counter()).isNull();
    }

    @Test
    void doesNotPublishSuccessfulBusinessFactAfterRollback() {
        org.springframework.transaction.support.TransactionSynchronizationManager
                .setActualTransactionActive(true);
        org.springframework.transaction.support.TransactionSynchronizationManager
                .initSynchronization();
        try {
            metrics.completed(SalesMetrics.Channel.POS, ElectronicDocumentType.DOC_EQUIV_POS,
                    BigDecimal.TEN, 1);
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(
                            org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));

            assertThat(registry.find(BusinessMetricNames.SALES_OPERATIONS).counter()).isNull();
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .clearSynchronization();
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .setActualTransactionActive(false);
        }
    }
}
