package com.vetsoftware.app.infrastructure.observability.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.ElectronicDocumentJpaRepository;
import com.vetsoftware.app.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.vetsoftware.app.inventory.infrastructure.persistence.StockLotJpaRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BusinessGaugeMetricsTest {

    @Test
    void refreshesCachedBacklogAndInventoryValues() {
        ElectronicDocumentJpaRepository documents = mock(ElectronicDocumentJpaRepository.class);
        StockBalanceJpaRepository balances = mock(StockBalanceJpaRepository.class);
        StockLotJpaRepository lots = mock(StockLotJpaRepository.class);
        Instant instant = Instant.parse("2026-07-28T15:00:00Z");
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        LocalDateTime currentInBogota = LocalDateTime.of(2026, 7, 28, 10, 0);
        LocalDate todayInBogota = LocalDate.of(2026, 7, 28);

        when(documents.countBacklogSince(DianStatus.PENDIENTE, currentInBogota.minusMinutes(15)))
                .thenReturn(2L);
        when(documents.countBacklogBetween(
                DianStatus.PENDIENTE, currentInBogota.minusHours(1), currentInBogota.minusMinutes(15)))
                .thenReturn(3L);
        when(documents.countBacklogBefore(DianStatus.PENDIENTE, currentInBogota.minusHours(1)))
                .thenReturn(4L);
        when(balances.countLowStock()).thenReturn(5L);
        when(lots.countExpiredBefore(todayInBogota)).thenReturn(6L);
        when(lots.countExpiringBetweenInclusive(todayInBogota, todayInBogota.plusDays(7)))
                .thenReturn(7L);
        when(lots.countExpiringAfterUntil(todayInBogota.plusDays(7), todayInBogota.plusDays(30)))
                .thenReturn(8L);

        BusinessGaugeMetrics metrics = new BusinessGaugeMetrics(documents, balances, lots, clock);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.config().meterFilter(new BusinessMetricCardinalityFilter());
        metrics.bindTo(registry);
        metrics.refresh();

        assertThat(registry.get(BusinessMetricNames.DIAN_BACKLOG)
                .tags("status", "pending", "age", "lt_15m").gauge().value()).isEqualTo(2);
        assertThat(registry.get(BusinessMetricNames.DIAN_BACKLOG)
                .tags("status", "pending", "age", "from_15m_to_1h").gauge().value()).isEqualTo(3);
        assertThat(registry.get(BusinessMetricNames.DIAN_BACKLOG)
                .tags("status", "pending", "age", "gt_1h").gauge().value()).isEqualTo(4);
        assertThat(registry.get(BusinessMetricNames.INVENTORY_LOW_STOCK).gauge().value()).isEqualTo(5);
        assertThat(registry.get(BusinessMetricNames.INVENTORY_EXPIRING_LOTS)
                .tag("age", "expired").gauge().value()).isEqualTo(6);
        assertThat(registry.get(BusinessMetricNames.INVENTORY_EXPIRING_LOTS)
                .tag("age", "from_0_to_7d").gauge().value()).isEqualTo(7);
        assertThat(registry.get(BusinessMetricNames.INVENTORY_EXPIRING_LOTS)
                .tag("age", "from_8_to_30d").gauge().value()).isEqualTo(8);
        assertThat(registry.get(BusinessMetricNames.SNAPSHOT_AGE).gauge().value()).isZero();

        PrometheusMeterRegistry prometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheus.config().meterFilter(new BusinessMetricCardinalityFilter());
        metrics.bindTo(prometheus);
        assertThat(prometheus.scrape())
                .contains("vetsoftware_business_dian_backlog_documents")
                .contains("vetsoftware_business_inventory_low_stock_products")
                .contains("vetsoftware_business_inventory_expiring_lots")
                .contains("vetsoftware_business_metrics_snapshot_age_seconds");
    }
}
