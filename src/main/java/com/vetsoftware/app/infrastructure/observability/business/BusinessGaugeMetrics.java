package com.vetsoftware.app.infrastructure.observability.business;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.ElectronicDocumentJpaRepository;
import com.vetsoftware.app.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.vetsoftware.app.inventory.infrastructure.persistence.StockLotJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Snapshot cacheado de estados de negocio. Prometheus lee memoria; las
 * consultas agregadas se ejecutan una vez por intervalo para que aumentar la
 * frecuencia de scrape no aumente la carga SQL.
 */
@Component
public class BusinessGaugeMetrics implements MeterBinder {

    private static final Logger log = LoggerFactory.getLogger(BusinessGaugeMetrics.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");

    private final ElectronicDocumentJpaRepository electronicDocuments;
    private final StockBalanceJpaRepository stockBalances;
    private final StockLotJpaRepository stockLots;
    private final Clock clock;
    private final Map<DianStatus, BacklogValues> backlog = new EnumMap<>(DianStatus.class);
    private final AtomicLong lowStock = new AtomicLong();
    private final AtomicLong expiredLots = new AtomicLong();
    private final AtomicLong expiringSevenDays = new AtomicLong();
    private final AtomicLong expiringThirtyDays = new AtomicLong();
    private final AtomicLong lastSuccessfulRefreshEpochSecond = new AtomicLong();

    @Autowired
    public BusinessGaugeMetrics(ElectronicDocumentJpaRepository electronicDocuments,
            StockBalanceJpaRepository stockBalances, StockLotJpaRepository stockLots) {
        this(electronicDocuments, stockBalances, stockLots, Clock.systemUTC());
    }

    BusinessGaugeMetrics(ElectronicDocumentJpaRepository electronicDocuments,
            StockBalanceJpaRepository stockBalances, StockLotJpaRepository stockLots, Clock clock) {
        this.electronicDocuments = electronicDocuments;
        this.stockBalances = stockBalances;
        this.stockLots = stockLots;
        this.clock = clock;
        backlog.put(DianStatus.PENDIENTE, new BacklogValues());
        backlog.put(DianStatus.CONTINGENCIA, new BacklogValues());
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        bindBacklog(registry, DianStatus.PENDIENTE, "pending");
        bindBacklog(registry, DianStatus.CONTINGENCIA, "contingency");
        Gauge.builder(BusinessMetricNames.INVENTORY_LOW_STOCK, lowStock, AtomicLong::doubleValue)
                .baseUnit("products")
                .description("Productos activos cuyo saldo está por debajo del mínimo")
                .register(registry);
        bindLotGauge(registry, expiredLots, "expired");
        bindLotGauge(registry, expiringSevenDays, "from_0_to_7d");
        bindLotGauge(registry, expiringThirtyDays, "from_8_to_30d");
        Gauge.builder(BusinessMetricNames.SNAPSHOT_AGE, this,
                BusinessGaugeMetrics::snapshotAgeSeconds).baseUnit("seconds")
                .description("Edad del último snapshot exitoso de métricas de estado de negocio")
                .register(registry);
    }

    @Scheduled(initialDelayString = "${vetsoftware.observability.business-metrics.initial-delay-ms:10000}", fixedDelayString = "${vetsoftware.observability.business-metrics.snapshot-refresh-ms:60000}")
    @Transactional(readOnly = true)
    public void refresh() {
        try {
            Instant now = clock.instant();
            LocalDateTime current = LocalDateTime.ofInstant(now, BUSINESS_ZONE);
            LocalDateTime fifteenMinutesAgo = current.minusMinutes(15);
            LocalDateTime oneHourAgo = current.minusHours(1);
            BacklogSnapshot pending = loadBacklog(DianStatus.PENDIENTE, fifteenMinutesAgo,
                    oneHourAgo);
            BacklogSnapshot contingency = loadBacklog(DianStatus.CONTINGENCIA, fifteenMinutesAgo,
                    oneHourAgo);

            LocalDate today = LocalDate.ofInstant(now, BUSINESS_ZONE);
            long newLowStock = stockBalances.countLowStock();
            long newExpiredLots = stockLots.countExpiredBefore(today);
            long newExpiringSevenDays = stockLots.countExpiringBetweenInclusive(today,
                    today.plusDays(7));
            long newExpiringThirtyDays = stockLots.countExpiringAfterUntil(today.plusDays(7),
                    today.plusDays(30));

            backlog.get(DianStatus.PENDIENTE).set(pending);
            backlog.get(DianStatus.CONTINGENCIA).set(contingency);
            lowStock.set(newLowStock);
            expiredLots.set(newExpiredLots);
            expiringSevenDays.set(newExpiringSevenDays);
            expiringThirtyDays.set(newExpiringThirtyDays);
            lastSuccessfulRefreshEpochSecond.set(now.getEpochSecond());
        } catch (RuntimeException exception) {
            log.warn(
                    "No se pudo actualizar el snapshot de métricas de negocio; se conserva el último valor:"
                            + " {}",
                    exception.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshWhenReady() {
        refresh();
    }

    private BacklogSnapshot loadBacklog(DianStatus status, LocalDateTime fifteenMinutesAgo,
            LocalDateTime oneHourAgo) {
        return new BacklogSnapshot(electronicDocuments.countBacklogSince(status, fifteenMinutesAgo),
                electronicDocuments.countBacklogBetween(status, oneHourAgo, fifteenMinutesAgo),
                electronicDocuments.countBacklogBefore(status, oneHourAgo));
    }

    private void bindBacklog(MeterRegistry registry, DianStatus status, String statusValue) {
        BacklogValues values = backlog.get(status);
        bindBacklogAge(registry, values.lessThanFifteenMinutes, statusValue, "lt_15m");
        bindBacklogAge(registry, values.fromFifteenMinutesToOneHour, statusValue, "from_15m_to_1h");
        bindBacklogAge(registry, values.moreThanOneHour, statusValue, "gt_1h");
    }

    private static void bindBacklogAge(MeterRegistry registry, AtomicLong value, String status,
            String age) {
        Gauge.builder(BusinessMetricNames.DIAN_BACKLOG, value, AtomicLong::doubleValue)
                .baseUnit("documents")
                .description("Documentos pendientes de resolución DIAN por estado y antigüedad")
                .tags("status", status, "age", age).register(registry);
    }

    private static void bindLotGauge(MeterRegistry registry, AtomicLong value, String age) {
        Gauge.builder(BusinessMetricNames.INVENTORY_EXPIRING_LOTS, value, AtomicLong::doubleValue)
                .baseUnit("lots").description("Lotes con existencia vencidos o próximos a vencer")
                .tag("age", age).register(registry);
    }

    private double snapshotAgeSeconds() {
        long refreshedAt = lastSuccessfulRefreshEpochSecond.get();
        if (refreshedAt == 0) {
            return Double.NaN;
        }
        return ChronoUnit.SECONDS.between(Instant.ofEpochSecond(refreshedAt), clock.instant());
    }

    private record BacklogSnapshot(long lessThanFifteenMinutes, long fromFifteenMinutesToOneHour,
            long moreThanOneHour) {
    }

    private static final class BacklogValues {
        private final AtomicLong lessThanFifteenMinutes = new AtomicLong();
        private final AtomicLong fromFifteenMinutesToOneHour = new AtomicLong();
        private final AtomicLong moreThanOneHour = new AtomicLong();

        private void set(BacklogSnapshot snapshot) {
            lessThanFifteenMinutes.set(snapshot.lessThanFifteenMinutes());
            fromFifteenMinutesToOneHour.set(snapshot.fromFifteenMinutesToOneHour());
            moreThanOneHour.set(snapshot.moreThanOneHour());
        }
    }
}
