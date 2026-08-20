package com.vetsoftware.app.infrastructure.observability.business;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.infrastructure.persistence.ElectronicDocumentJpaRepository;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.vetsoftware.app.inventory.infrastructure.persistence.StockLotJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Snapshot cacheado de estados de negocio. Prometheus lee memoria; las
 * consultas agregadas se ejecutan una vez por intervalo para que aumentar la
 * frecuencia de scrape no aumente la carga SQL.
 */
@Component
public class BusinessGaugeMetrics implements MeterBinder {

    private static final Logger log = LoggerFactory.getLogger(BusinessGaugeMetrics.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private static final String JOB_NAME = "business.metrics.snapshot";

    /**
     * Mismos valores por defecto que {@code dian.contingency.*} en application.yml:
     * la métrica de agotados tiene que decidir con los mismos límites que el job, o
     * contaría documentos distintos de los que el job descarta.
     */
    private static final int DEFAULT_MAX_ATTEMPTS = 4;
    private static final long DEFAULT_DEADLINE_HOURS = 48;

    private final ElectronicDocumentJpaRepository electronicDocuments;
    private final StockBalanceJpaRepository stockBalances;
    private final StockLotJpaRepository stockLots;
    private final Clock clock;
    private final int contingencyMaxAttempts;
    private final long contingencyDeadlineHours;
    private final TransactionOperations transactions;
    private final ScheduledJobTelemetry telemetry;
    private final Map<DianStatus, BacklogValues> backlog = new EnumMap<>(DianStatus.class);
    private final AtomicLong lowStock = new AtomicLong();
    private final AtomicLong expiredLots = new AtomicLong();
    private final AtomicLong expiringSevenDays = new AtomicLong();
    private final AtomicLong expiringThirtyDays = new AtomicLong();
    private final AtomicLong contingencyExhausted = new AtomicLong();
    private final AtomicLong lastSuccessfulRefreshEpochSecond = new AtomicLong();

    /**
     * Constructor que usa el contenedor. La transacción se abre desde dentro del
     * cuerpo con {@link TransactionOperations} y no con {@code @Transactional}
     * sobre {@link #refresh()}: el proxy transaccional envuelve al método por
     * fuera, así que un fallo al abrirla —{@code CannotCreateTransactionException}
     * cuando el pool no da conexión— se lanzaría <b>antes</b> de que
     * {@link ScheduledJobTelemetry} pudiera etiquetar la observación, y la alerta
     * de trabajos programados, que exige {@code job_name}, lo ignoraría.
     */
    @Autowired
    public BusinessGaugeMetrics(ElectronicDocumentJpaRepository electronicDocuments,
            StockBalanceJpaRepository stockBalances, StockLotJpaRepository stockLots,
            @Value("${dian.contingency.max-attempts:" + DEFAULT_MAX_ATTEMPTS
                    + "}") int contingencyMaxAttempts,
            @Value("${dian.contingency.deadline-hours:" + DEFAULT_DEADLINE_HOURS
                    + "}") long contingencyDeadlineHours,
            PlatformTransactionManager transactionManager, ScheduledJobTelemetry telemetry) {
        this(electronicDocuments, stockBalances, stockLots, Clock.systemUTC(),
                contingencyMaxAttempts, contingencyDeadlineHours,
                readOnlyTransactions(transactionManager), telemetry);
    }

    /**
     * Conveniencia fuera del contenedor: ejecuta las consultas sin transacción
     * propia y sin observación. Producción usa el constructor anotado con
     * {@link Autowired}.
     */
    public BusinessGaugeMetrics(ElectronicDocumentJpaRepository electronicDocuments,
            StockBalanceJpaRepository stockBalances, StockLotJpaRepository stockLots,
            int contingencyMaxAttempts, long contingencyDeadlineHours) {
        this(electronicDocuments, stockBalances, stockLots, Clock.systemUTC(),
                contingencyMaxAttempts, contingencyDeadlineHours,
                TransactionOperations.withoutTransaction(), noopTelemetry());
    }

    BusinessGaugeMetrics(ElectronicDocumentJpaRepository electronicDocuments,
            StockBalanceJpaRepository stockBalances, StockLotJpaRepository stockLots, Clock clock) {
        this(electronicDocuments, stockBalances, stockLots, clock, DEFAULT_MAX_ATTEMPTS,
                DEFAULT_DEADLINE_HOURS);
    }

    BusinessGaugeMetrics(ElectronicDocumentJpaRepository electronicDocuments,
            StockBalanceJpaRepository stockBalances, StockLotJpaRepository stockLots, Clock clock,
            int contingencyMaxAttempts, long contingencyDeadlineHours) {
        this(electronicDocuments, stockBalances, stockLots, clock, contingencyMaxAttempts,
                contingencyDeadlineHours, TransactionOperations.withoutTransaction(),
                noopTelemetry());
    }

    BusinessGaugeMetrics(ElectronicDocumentJpaRepository electronicDocuments,
            StockBalanceJpaRepository stockBalances, StockLotJpaRepository stockLots, Clock clock,
            int contingencyMaxAttempts, long contingencyDeadlineHours,
            TransactionOperations transactions, ScheduledJobTelemetry telemetry) {
        this.electronicDocuments = electronicDocuments;
        this.stockBalances = stockBalances;
        this.stockLots = stockLots;
        this.clock = clock;
        this.contingencyMaxAttempts = contingencyMaxAttempts;
        this.contingencyDeadlineHours = contingencyDeadlineHours;
        this.transactions = transactions;
        this.telemetry = telemetry;
        backlog.put(DianStatus.PENDIENTE, new BacklogValues());
        backlog.put(DianStatus.CONTINGENCIA, new BacklogValues());
    }

    private static TransactionOperations readOnlyTransactions(
            PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(true);
        return template;
    }

    private static ScheduledJobTelemetry noopTelemetry() {
        return new ScheduledJobTelemetry(ObservationRegistry.NOOP);
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
        // Sin etiquetas a propósito: una sola serie. El identificador de empresa
        // aquí multiplicaría series por tenant sin responder ninguna pregunta que
        // la traza o el log del job no respondan ya.
        Gauge.builder(BusinessMetricNames.DIAN_CONTINGENCY_EXHAUSTED, contingencyExhausted,
                AtomicLong::doubleValue).baseUnit("documents")
                .description(
                        "Documentos en contingencia DIAN cuyos reintentos automáticos se agotaron"
                                + " y esperan reemisión manual")
                .register(registry);
        Gauge.builder(BusinessMetricNames.SNAPSHOT_AGE, this,
                BusinessGaugeMetrics::snapshotAgeSeconds).baseUnit("seconds")
                .description("Edad del último snapshot exitoso de métricas de estado de negocio")
                .register(registry);
    }

    @Scheduled(initialDelayString = "${vetsoftware.observability.business-metrics.initial-delay-ms:10000}", fixedDelayString = "${vetsoftware.observability.business-metrics.snapshot-refresh-ms:60000}")
    public void refresh() {
        try {
            telemetry.observe(JOB_NAME, this::loadSnapshot);
        } catch (RuntimeException exception) {
            // Resiliencia deliberada: se conserva el último snapshot y su edad crece
            // hasta que VetSoftwareBusinessMetricsSnapshotStale lo delate. Relanzar
            // aquí rompería además el listener de ApplicationReadyEvent.
            log.warn("No se pudo actualizar el snapshot de métricas de negocio;"
                    + " se conserva el último valor", exception);
        }
    }

    private ScheduledJobTelemetry.Outcome loadSnapshot() {
        Instant now = clock.instant();
        Snapshot snapshot = Objects.requireNonNull(transactions.execute(status -> read(now)),
                "La carga del snapshot debe devolver valores");
        publish(snapshot, now);
        return ScheduledJobTelemetry.Outcome.SUCCESS;
    }

    private Snapshot read(Instant now) {
        LocalDateTime current = LocalDateTime.ofInstant(now, BUSINESS_ZONE);
        LocalDateTime fifteenMinutesAgo = current.minusMinutes(15);
        LocalDateTime oneHourAgo = current.minusHours(1);
        LocalDate today = LocalDate.ofInstant(now, BUSINESS_ZONE);
        return new Snapshot(loadBacklog(DianStatus.PENDIENTE, fifteenMinutesAgo, oneHourAgo),
                loadBacklog(DianStatus.CONTINGENCIA, fifteenMinutesAgo, oneHourAgo),
                loadContingencyExhausted(now), stockBalances.countLowStock(),
                stockLots.countExpiredBefore(today),
                stockLots.countExpiringBetweenInclusive(today, today.plusDays(7)),
                stockLots.countExpiringAfterUntil(today.plusDays(7), today.plusDays(30)));
    }

    /**
     * Los medidores solo se mueven cuando las consultas terminaron todas bien: un
     * snapshot a medias mezclaría valores de dos ciclos distintos.
     */
    private void publish(Snapshot snapshot, Instant now) {
        backlog.get(DianStatus.PENDIENTE).set(snapshot.pending());
        backlog.get(DianStatus.CONTINGENCIA).set(snapshot.contingency());
        lowStock.set(snapshot.lowStock());
        expiredLots.set(snapshot.expiredLots());
        expiringSevenDays.set(snapshot.expiringSevenDays());
        expiringThirtyDays.set(snapshot.expiringThirtyDays());
        contingencyExhausted.set(snapshot.contingencyExhausted());
        lastSuccessfulRefreshEpochSecond.set(now.getEpochSecond());
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

    /**
     * Conteo de documentos en contingencia que el job ya no reintenta.
     *
     * <p>
     * El umbral de plazo se calcula en la <b>zona por defecto del proceso</b>, no
     * en {@link #BUSINESS_ZONE}, porque {@code ContingencyRetryJob} decide con
     * {@code LocalDateTime.now()} y {@code created_date} se persiste con esa misma
     * zona. Usar aquí America/Bogota desplazaría el umbral cinco horas y la métrica
     * contaría una población distinta de la que el job descarta.
     */
    private long loadContingencyExhausted(Instant now) {
        LocalDateTime deadlineThreshold = LocalDateTime.ofInstant(now, ZoneId.systemDefault())
                .minusHours(contingencyDeadlineHours);
        return electronicDocuments.countRetriesExhausted(DianStatus.CONTINGENCIA, deadlineThreshold,
                contingencyMaxAttempts);
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

    private record Snapshot(BacklogSnapshot pending, BacklogSnapshot contingency,
            long contingencyExhausted, long lowStock, long expiredLots, long expiringSevenDays,
            long expiringThirtyDays) {
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
