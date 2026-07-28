package com.vetsoftware.app.infrastructure.observability.business;

import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.cashregister.application.port.out.CashMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.inventory.application.port.out.InventoryMetrics;
import com.vetsoftware.app.inventory.domain.StockMovementType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Adaptador único de los contratos de telemetría comercial. Todos sus tags provienen de enums
 * o transformaciones cerradas y son validados además por {@link BusinessMetricCardinalityFilter}.
 */
@Component
public class MicrometerBusinessMetrics
        implements SalesMetrics, BillingMetrics, InventoryMetrics, AppointmentMetrics, CashMetrics {

    private final AfterCommitMetricRecorder recorder;

    private final Meter.MeterProvider<Counter> salesOperations;
    private final Meter.MeterProvider<DistributionSummary> salesAmount;
    private final Meter.MeterProvider<DistributionSummary> salesLines;
    private final Meter.MeterProvider<Counter> dianTransmissions;
    private final Meter.MeterProvider<Timer> dianTransmissionDuration;
    private final Meter.MeterProvider<Counter> inventoryMovements;
    private final Meter.MeterProvider<DistributionSummary> inventoryUnits;
    private final Meter.MeterProvider<Counter> appointmentTransitions;
    private final Meter.MeterProvider<Counter> cashSessions;
    private final Meter.MeterProvider<DistributionSummary> cashClosingDifference;

    public MicrometerBusinessMetrics(MeterRegistry registry, AfterCommitMetricRecorder recorder) {
        this.recorder = recorder;
        salesOperations = Counter.builder(BusinessMetricNames.SALES_OPERATIONS)
                .baseUnit("operations")
                .description("Operaciones comerciales de venta por resultado, canal y tipo de documento")
                .withRegistry(registry);
        salesAmount = DistributionSummary.builder(BusinessMetricNames.SALES_AMOUNT)
                .baseUnit("cop")
                .description("Valor operativo de ventas completadas; no sustituye la contabilidad")
                .withRegistry(registry);
        salesLines = DistributionSummary.builder(BusinessMetricNames.SALES_LINES)
                .baseUnit("lines")
                .description("Cantidad de líneas comerciales por venta completada")
                .withRegistry(registry);
        dianTransmissions = Counter.builder(BusinessMetricNames.DIAN_TRANSMISSIONS)
                .baseUnit("transmissions")
                .description("Resultados persistidos de comunicación con la DIAN")
                .withRegistry(registry);
        dianTransmissionDuration = Timer.builder(BusinessMetricNames.DIAN_TRANSMISSION_DURATION)
                .description("Duración extremo a extremo del intento de comunicación con la DIAN")
                .withRegistry(registry);
        inventoryMovements = Counter.builder(BusinessMetricNames.INVENTORY_MOVEMENTS)
                .baseUnit("movements")
                .description("Movimientos lógicos de inventario por tipo y resultado")
                .withRegistry(registry);
        inventoryUnits = DistributionSummary.builder(BusinessMetricNames.INVENTORY_UNITS)
                .baseUnit("units")
                .description("Unidades base afectadas por movimientos exitosos de inventario")
                .withRegistry(registry);
        appointmentTransitions = Counter.builder(BusinessMetricNames.APPOINTMENT_TRANSITIONS)
                .baseUnit("transitions")
                .description("Transiciones persistidas del estado de citas")
                .withRegistry(registry);
        cashSessions = Counter.builder(BusinessMetricNames.CASH_SESSIONS)
                .baseUnit("sessions")
                .description("Aperturas y cierres persistidos de caja")
                .withRegistry(registry);
        cashClosingDifference = DistributionSummary.builder(BusinessMetricNames.CASH_CLOSING_DIFFERENCE)
                .baseUnit("cop")
                .description("Valor absoluto de diferencias encontradas al cerrar caja")
                .withRegistry(registry);
    }

    @Override
    public void completed(
            SalesMetrics.Channel channel,
            ElectronicDocumentType documentType,
            BigDecimal amount,
            int lineCount) {
        BigDecimal immutableAmount = nonNegative(amount);
        int immutableLineCount = Math.max(0, lineCount);
        recorder.recordAfterCommit(() -> {
            String documentTypeValue = documentType(documentType);
            salesOperations.withTags(
                    "result", "completed",
                    "channel", channel.value(),
                    "document.type", documentTypeValue).increment();
            salesAmount.withTags(
                    "channel", channel.value(),
                    "document.type", documentTypeValue).record(immutableAmount.doubleValue());
            salesLines.withTags(
                    "channel", channel.value(),
                    "document.type", documentTypeValue).record(immutableLineCount);
        });
    }

    @Override
    public void failed(
            SalesMetrics.Channel channel,
            ElectronicDocumentType documentType,
            SalesMetrics.Result result) {
        recorder.recordNow(() -> salesOperations.withTags(
                "result", result.value(),
                "channel", channel.value(),
                "document.type", documentType(documentType)).increment());
    }

    @Override
    public void finished(DianStatus status, Origin origin, ElectronicDocumentType documentType, Duration duration) {
        Duration immutableDuration = nonNegative(duration);
        recorder.recordAfterCommit(() -> {
            String result = dianResult(status);
            String documentTypeValue = documentType(documentType);
            dianTransmissions.withTags(
                    "result", result,
                    "origin", origin.value(),
                    "document.type", documentTypeValue).increment();
            dianTransmissionDuration.withTags(
                    "result", result,
                    "origin", origin.value()).record(immutableDuration);
        });
    }

    @Override
    public void failed(Origin origin, ElectronicDocumentType documentType, Duration duration) {
        Duration immutableDuration = nonNegative(duration);
        recorder.recordNow(() -> {
            dianTransmissions.withTags(
                    "result", "error",
                    "origin", origin.value(),
                    "document.type", documentType(documentType)).increment();
            dianTransmissionDuration.withTags(
                    "result", "error",
                    "origin", origin.value()).record(immutableDuration);
        });
    }

    @Override
    public void movement(StockMovementType movementType, InventoryMetrics.Result result, int units) {
        Runnable action = () -> {
            String type = lower(movementType);
            inventoryMovements.withTags(
                    "movement.type", type,
                    "result", result.value()).increment();
            if (result == InventoryMetrics.Result.SUCCESS && units > 0) {
                inventoryUnits.withTags("movement.type", type).record(units);
            }
        };
        if (result == InventoryMetrics.Result.SUCCESS) {
            recorder.recordAfterCommit(action);
        } else {
            recorder.recordNow(action);
        }
    }

    @Override
    public void transitioned(AppointmentStatus status, AppointmentMetrics.Channel channel) {
        recorder.recordAfterCommit(() -> appointmentTransitions.withTags(
                "status", lower(status),
                "channel", channel.value()).increment());
    }

    @Override
    public void opened() {
        recorder.recordAfterCommit(() -> cashSessions.withTags(
                "event", "opened",
                "result", "success").increment());
    }

    @Override
    public void closed(List<BigDecimal> differences) {
        List<BigDecimal> immutableDifferences = differences == null
                ? List.of()
                : differences.stream().map(MicrometerBusinessMetrics::zeroIfNull).toList();
        recorder.recordAfterCommit(() -> {
            boolean hasDifference = immutableDifferences.stream().anyMatch(value -> value.signum() != 0);
            cashSessions.withTags(
                    "event", "closed",
                    "result", hasDifference ? "difference" : "success").increment();
            if (immutableDifferences.isEmpty()) {
                cashClosingDifference.withTags("direction", "balanced").record(0);
                return;
            }
            for (BigDecimal difference : immutableDifferences) {
                cashClosingDifference.withTags("direction", direction(difference))
                        .record(difference.abs().doubleValue());
            }
        });
    }

    private static String documentType(ElectronicDocumentType type) {
        return type == null ? "unknown" : lower(type);
    }

    private static String dianResult(DianStatus status) {
        return switch (status) {
            case VALIDADO -> "validated";
            case RECHAZADO -> "rejected";
            case CONTINGENCIA -> "contingency";
            case PENDIENTE -> "pending";
            case NO_ELECTRONICO -> throw new IllegalArgumentException(
                    "NO_ELECTRONICO no representa una transmisión DIAN");
        };
    }

    private static String direction(BigDecimal difference) {
        return switch (difference.signum()) {
            case -1 -> "shortage";
            case 0 -> "balanced";
            default -> "surplus";
        };
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private static Duration nonNegative(Duration value) {
        return value == null || value.isNegative() ? Duration.ZERO : value;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
