package com.vetsoftware.app.infrastructure.observability.business;

import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.cashregister.application.port.out.CashMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.DocumentDeliveryMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.inventory.application.port.out.InventoryMetrics;
import com.vetsoftware.app.inventory.domain.StockMovementType;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionEntitlementMetrics;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionLifecycleMetrics;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentMetrics;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
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
 * Adaptador único de los contratos de telemetría comercial. Todos sus tags
 * provienen de enums o transformaciones cerradas y son validados además por
 * {@link BusinessMetricCardinalityFilter}.
 */
@Component
public class MicrometerBusinessMetrics
        implements
            SalesMetrics,
            BillingMetrics,
            DocumentDeliveryMetrics,
            InventoryMetrics,
            AppointmentMetrics,
            CashMetrics,
            PlatformAccessMetrics,
            SubscriptionBillingMetrics,
            SubscriptionPaymentMetrics,
            SubscriptionLifecycleMetrics,
            SubscriptionEntitlementMetrics {

    private final AfterCommitMetricRecorder recorder;

    private final Meter.MeterProvider<Counter> salesOperations;
    private final Meter.MeterProvider<DistributionSummary> salesAmount;
    private final Meter.MeterProvider<DistributionSummary> salesLines;
    private final Meter.MeterProvider<Counter> dianTransmissions;
    private final Meter.MeterProvider<Timer> dianTransmissionDuration;
    private final Meter.MeterProvider<Counter> documentDeliveries;
    private final Meter.MeterProvider<Counter> inventoryMovements;
    private final Meter.MeterProvider<DistributionSummary> inventoryUnits;
    private final Meter.MeterProvider<Counter> appointmentTransitions;
    private final Meter.MeterProvider<Counter> cashSessions;
    private final Meter.MeterProvider<DistributionSummary> cashClosingDifference;
    private final Meter.MeterProvider<Counter> systemUserRequests;
    private final Meter.MeterProvider<Counter> systemUserApprovals;
    private final Meter.MeterProvider<Counter> systemUserInvitations;
    /**
     * Sin {@code MeterProvider}: se registra de una vez en el constructor. No tiene
     * etiquetas, así que no hay nada que diferir, y pre-registrarlo a cero es lo
     * que hace que la alerta {@code increase(...) > 0} funcione desde el primer
     * scrape en lugar de depender de que la serie nazca justo durante el incidente.
     * Es el mismo argumento con el que el filtro de cardinalidad pre-registra sus
     * contadores de descarte.
     */
    private final Counter systemUserProvisioned;

    // Dinero de suscripciones (#606). Siete medidores, ninguno con companyId.
    private final Meter.MeterProvider<Counter> subscriptionCharges;
    private final Meter.MeterProvider<DistributionSummary> subscriptionChargedAmount;
    private final Meter.MeterProvider<Counter> subscriptionDocuments;
    private final Meter.MeterProvider<Counter> subscriptionPayments;
    private final Meter.MeterProvider<Counter> subscriptionApplications;
    private final Meter.MeterProvider<Counter> subscriptionStatusTransitions;
    private final Meter.MeterProvider<Counter> subscriptionEntitlementRecalculations;

    public MicrometerBusinessMetrics(MeterRegistry registry, AfterCommitMetricRecorder recorder) {
        this.recorder = recorder;
        salesOperations = Counter.builder(BusinessMetricNames.SALES_OPERATIONS)
                .baseUnit("operations")
                .description(
                        "Operaciones comerciales de venta por resultado, canal y tipo de documento")
                .withRegistry(registry);
        salesAmount = DistributionSummary.builder(BusinessMetricNames.SALES_AMOUNT).baseUnit("cop")
                .description("Valor operativo de ventas completadas; no sustituye la contabilidad")
                .withRegistry(registry);
        salesLines = DistributionSummary.builder(BusinessMetricNames.SALES_LINES).baseUnit("lines")
                .description("Cantidad de líneas comerciales por venta completada")
                .withRegistry(registry);
        dianTransmissions = Counter.builder(BusinessMetricNames.DIAN_TRANSMISSIONS)
                .baseUnit("transmissions")
                .description("Resultados persistidos de comunicación con la DIAN")
                .withRegistry(registry);
        dianTransmissionDuration = Timer.builder(BusinessMetricNames.DIAN_TRANSMISSION_DURATION)
                .description("Duración extremo a extremo del intento de comunicación con la DIAN")
                .withRegistry(registry);
        // Sin baseUnit, a diferencia del resto de contadores de este adaptador. En
        // ellos el nombre ya termina en la unidad («dian.transmissions» + unidad
        // «transmissions») y el exportador de Prometheus no la duplica. Aquí el nombre
        // publicado es singular —«document.delivery», constante ya fijada en
        // BusinessMetricNames— así que declarar «deliveries» produciría
        // vetsoftware_business_document_delivery_deliveries_total: un nombre que nadie
        // adivina al escribir la alerta. Omitirla deja el esperado
        // vetsoftware_business_document_delivery_total. La unidad de un contador de
        // entregas es adimensional, así que no se pierde información.
        documentDeliveries = Counter.builder(BusinessMetricNames.DOCUMENT_DELIVERY)
                .description(
                        "Entregas por correo de la representación gráfica, por resultado; un fallo"
                                + " es pérdida definitiva porque no hay reintento")
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
                .baseUnit("transitions").description("Transiciones persistidas del estado de citas")
                .withRegistry(registry);
        cashSessions = Counter.builder(BusinessMetricNames.CASH_SESSIONS).baseUnit("sessions")
                .description("Aperturas y cierres persistidos de caja").withRegistry(registry);
        cashClosingDifference = DistributionSummary
                .builder(BusinessMetricNames.CASH_CLOSING_DIFFERENCE).baseUnit("cop")
                .description("Valor absoluto de diferencias encontradas al cerrar caja")
                .withRegistry(registry);
        systemUserRequests = Counter.builder(BusinessMetricNames.SYSTEM_USER_REQUESTS)
                .baseUnit("requests")
                .description("Solicitudes publicas de alta de superadministrador, por resultado")
                .withRegistry(registry);
        systemUserApprovals = Counter.builder(BusinessMetricNames.SYSTEM_USER_APPROVALS)
                .baseUnit("approvals")
                .description("Resoluciones del enlace del aprobador, por resultado")
                .withRegistry(registry);
        systemUserInvitations = Counter.builder(BusinessMetricNames.SYSTEM_USER_INVITATIONS)
                .baseUnit("invitations")
                .description("Ciclo de vida de las invitaciones de plataforma, por resultado")
                .withRegistry(registry);
        // Sin baseUnit, y por el mismo motivo que document.delivery: el nombre no
        // termina en un sustantivo de unidad, asi que declarar una produciria
        // ..._provisioned_admins_total, un nombre que nadie adivina al escribir la
        // alerta. Sin ella queda vetsoftware_business_system_user_provisioned_total,
        // que es literalmente lo que dice la alerta.
        systemUserProvisioned = Counter.builder(BusinessMetricNames.SYSTEM_USER_PROVISIONED)
                .description("Cuentas con control total de la plataforma creadas por invitacion")
                .register(registry);
        subscriptionCharges = Counter.builder(BusinessMetricNames.SUBSCRIPTION_CHARGES)
                .baseUnit("charges")
                .description("Cargos de suscripcion devengados o anulados, por clase y resultado")
                .withRegistry(registry);
        subscriptionChargedAmount = DistributionSummary
                .builder(BusinessMetricNames.SUBSCRIPTION_CHARGED_AMOUNT).baseUnit("cop")
                .description("Importe devengado por clase de cargo; el signo va en charge.sign"
                        + " porque DistributionSummary descarta los negativos")
                .withRegistry(registry);
        subscriptionDocuments = Counter.builder(BusinessMetricNames.SUBSCRIPTION_DOCUMENTS)
                .baseUnit("documents")
                .description("Cuentas de cobro de suscripcion por estado de emision y resultado")
                .withRegistry(registry);
        subscriptionPayments = Counter.builder(BusinessMetricNames.SUBSCRIPTION_PAYMENTS)
                .baseUnit("payments")
                .description("Pagos de suscripcion registrados, por medio y estado alcanzado")
                .withRegistry(registry);
        subscriptionApplications = Counter.builder(BusinessMetricNames.SUBSCRIPTION_APPLICATIONS)
                .baseUnit("applications")
                .description("Imputaciones contra cuentas de cobro, por clase de fuente")
                .withRegistry(registry);
        subscriptionStatusTransitions = Counter
                .builder(BusinessMetricNames.SUBSCRIPTION_STATUS_TRANSITIONS)
                .baseUnit("transitions")
                .description("Transiciones persistidas del estado del contrato;"
                        + " to.status=read_only es un cliente sin escritura")
                .withRegistry(registry);
        subscriptionEntitlementRecalculations = Counter
                .builder(BusinessMetricNames.SUBSCRIPTION_ENTITLEMENT_RECALCULATIONS)
                .baseUnit("recalculations")
                .description("Recalculos de entitlements disparados por un cambio de contrato")
                .withRegistry(registry);
    }

    /**
     * El canal decide el momento de publicación, igual que el resultado lo decide
     * en {@link #movement}:
     *
     * <ul>
     * <li><b>POS</b> va con {@code recordAfterCommit}: es la regla general de no
     * publicar un éxito de negocio que un rollback podría deshacer.</li>
     * <li><b>OPEN_ACCOUNT</b> va con {@code recordNow} y NO es una excepción
     * caprichosa. Ese canal se publica desde
     * {@code ClosedAccountEmissionCompleter}, que corre dentro del callback
     * {@code afterCommit} del cierre de cuenta (A1). Ahí no queda ningún commit por
     * esperar —el documento y el desenlace DIAN ya están persistidos, cada uno en
     * su propia transacción corta— pero, y esto es lo que obliga al cambio, Spring
     * todavía NO ha limpiado el {@code TransactionSynchronizationManager}:
     * {@code cleanupAfterCompletion()} corre en el {@code finally} externo de
     * {@code processCommit}, después de {@code triggerAfterCommit()}. Es decir,
     * {@code isActualTransactionActive()} sigue devolviendo {@code true} durante el
     * callback, así que {@code recordAfterCommit} tomaría la rama de
     * {@code registerSynchronization} en vez de caer a {@code recordNow} como
     * podría parecer. Y una sincronización registrada en ese instante ya no recibe
     * {@code afterCommit}: {@code triggerAfterCommit} itera sobre el snapshot que
     * devolvió {@code getSynchronizations()} antes de empezar. La métrica se
     * perdería en silencio — exactamente el defecto que este canal viene a
     * cerrar.</li>
     * </ul>
     */
    @Override
    public void completed(SalesMetrics.Channel channel, ElectronicDocumentType documentType,
            BigDecimal amount, int lineCount) {
        BigDecimal immutableAmount = nonNegative(amount);
        int immutableLineCount = Math.max(0, lineCount);
        Runnable action = () -> {
            String documentTypeValue = documentType(documentType);
            salesOperations.withTags("result", "completed", "channel", channel.value(),
                    "document.type", documentTypeValue).increment();
            salesAmount.withTags("channel", channel.value(), "document.type", documentTypeValue)
                    .record(immutableAmount.doubleValue());
            salesLines.withTags("channel", channel.value(), "document.type", documentTypeValue)
                    .record(immutableLineCount);
        };
        if (channel == SalesMetrics.Channel.OPEN_ACCOUNT) {
            recorder.recordNow(action);
        } else {
            recorder.recordAfterCommit(action);
        }
    }

    @Override
    public void failed(SalesMetrics.Channel channel, ElectronicDocumentType documentType,
            SalesMetrics.Result result) {
        recorder.recordNow(() -> salesOperations.withTags("result", result.value(), "channel",
                channel.value(), "document.type", documentType(documentType)).increment());
    }

    @Override
    public void finished(DianStatus status, Origin origin, ElectronicDocumentType documentType,
            Duration duration) {
        Duration immutableDuration = nonNegative(duration);
        recorder.recordAfterCommit(() -> {
            String result = dianResult(status);
            String documentTypeValue = documentType(documentType);
            dianTransmissions.withTags("result", result, "origin", origin.value(), "document.type",
                    documentTypeValue).increment();
            dianTransmissionDuration.withTags("result", result, "origin", origin.value())
                    .record(immutableDuration);
        });
    }

    @Override
    public void failed(Origin origin, ElectronicDocumentType documentType, Duration duration) {
        Duration immutableDuration = nonNegative(duration);
        recorder.recordNow(() -> {
            dianTransmissions.withTags("result", "error", "origin", origin.value(), "document.type",
                    documentType(documentType)).increment();
            dianTransmissionDuration.withTags("result", "error", "origin", origin.value())
                    .record(immutableDuration);
        });
    }

    /**
     * Ambos resultados de la entrega se publican con {@code recordNow} —y no con
     * {@code recordAfterCommit} como los hechos de venta— por dos razones:
     *
     * <ol>
     * <li><b>El fallo no puede quedar condicionado a un commit.</b> Es justo el
     * escenario en el que la transacción puede no confirmarse, y una métrica de
     * fallo que se pierde con el rollback deja el incidente invisible: exactamente
     * el defecto que este contador viene a cerrar.</li>
     * <li><b>El éxito tiene que compartir ciclo de vida con el fallo.</b> Los dos
     * brazos alimentan la misma razón {@code failed / (failed + success)}. Si el
     * numerador se publicara siempre y el denominador solo tras commit, la tasa de
     * error saldría inflada sin que nada fallara. Además el hecho medido —el
     * proveedor de correo aceptó el envío— es un efecto externo que ningún rollback
     * deshace, así que no aplica la regla de «no publicar éxitos antes del
     * commit».</li>
     * </ol>
     */
    @Override
    public void delivered() {
        recorder.recordNow(() -> documentDeliveries.withTags("result", "success").increment());
    }

    @Override
    public void deliveryFailed() {
        recorder.recordNow(() -> documentDeliveries.withTags("result", "failed").increment());
    }

    @Override
    public void movement(StockMovementType movementType, InventoryMetrics.Result result,
            int units) {
        Runnable action = () -> {
            String type = lower(movementType);
            inventoryMovements.withTags("movement.type", type, "result", result.value())
                    .increment();
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
        recorder.recordAfterCommit(() -> appointmentTransitions
                .withTags("status", lower(status), "channel", channel.value()).increment());
    }

    @Override
    public void opened() {
        recorder.recordAfterCommit(
                () -> cashSessions.withTags("event", "opened", "result", "success").increment());
    }

    @Override
    public void closed(List<BigDecimal> differences) {
        List<BigDecimal> immutableDifferences = differences == null
                ? List.of()
                : differences.stream().map(MicrometerBusinessMetrics::zeroIfNull).toList();
        recorder.recordAfterCommit(() -> {
            boolean hasDifference = immutableDifferences.stream()
                    .anyMatch(value -> value.signum() != 0);
            cashSessions
                    .withTags("event", "closed", "result", hasDifference ? "difference" : "success")
                    .increment();
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

    // ── Alta de superadministradores de plataforma ──────────────────────────
    //
    // Los exitos van con recordAfterCommit y las denegaciones con recordNow.
    // Publicar un alta de superadministrador que luego hace rollback es contar
    // una cuenta que no existe; una denegacion, en cambio, no tiene transaccion
    // que esperar.

    @Override
    public void requested(PlatformAccessMetrics.RequestResult result) {
        Runnable action = () -> systemUserRequests.withTags("result", result.value()).increment();
        if (result == PlatformAccessMetrics.RequestResult.SUCCESS) {
            recorder.recordAfterCommit(action);
        } else {
            recorder.recordNow(action);
        }
    }

    @Override
    public void resolved(PlatformAccessMetrics.ApprovalResult result) {
        Runnable action = () -> systemUserApprovals.withTags("result", result.value()).increment();
        if (result == PlatformAccessMetrics.ApprovalResult.APPROVED
                || result == PlatformAccessMetrics.ApprovalResult.REJECTED) {
            recorder.recordAfterCommit(action);
        } else {
            recorder.recordNow(action);
        }
    }

    @Override
    public void invitation(PlatformAccessMetrics.InvitationResult result) {
        Runnable action = () -> systemUserInvitations.withTags("result", result.value())
                .increment();
        if (result == PlatformAccessMetrics.InvitationResult.ACCEPTED) {
            recorder.recordAfterCommit(action);
        } else {
            // sent / failed / skipped se publican desde el hilo del pool de correo,
            // ya despues del commit: alli no queda transaccion que esperar y
            // recordAfterCommit tomaria la rama equivocada.
            recorder.recordNow(action);
        }
    }

    @Override
    public void provisioned() {
        recorder.recordAfterCommit(systemUserProvisioned::increment);
    }

    // -- Dinero de suscripciones (#606) ----------------------------------------
    //
    // Todo lo que representa un hecho persistido va por recordAfterCommit: contar
    // un cargo que despues hace rollback es publicar una venta que no existio, y
    // el cierre de mes es justo donde eso se paga caro. Los desenlaces que NO
    // persisten -las dos formas de rechazo de la emision- van por recordNow, y no
    // es una inconsistencia: nacen de una excepcion que revierte la transaccion,
    // asi que recordAfterCommit los descartaria a todos y el contador de rechazos
    // seria constantemente cero. Es el mismo reparto que ya usan `completed` y
    // `failed` de las ventas.

    @Override
    public void chargeAccrued(ChargeType chargeType, BigDecimal amount) {
        BigDecimal immutable = zeroIfNull(amount);
        recorder.recordAfterCommit(() -> {
            String type = lower(chargeType);
            subscriptionCharges.withTags("charge.type", type, "result", "completed").increment();
            subscriptionChargedAmount
                    .withTags("charge.type", type, "charge.sign", chargeSign(immutable))
                    .record(immutable.abs().doubleValue());
        });
    }

    @Override
    public void chargeVoided(ChargeType chargeType) {
        recorder.recordAfterCommit(() -> subscriptionCharges
                .withTags("charge.type", lower(chargeType), "result", "cancelled").increment());
    }

    @Override
    public void documentIssued(IssueStatus issueStatus) {
        recorder.recordAfterCommit(() -> subscriptionDocuments
                .withTags("issue.status", lower(issueStatus), "result", "completed").increment());
    }

    @Override
    public void documentVoided(IssueStatus issueStatus) {
        recorder.recordAfterCommit(() -> subscriptionDocuments
                .withTags("issue.status", lower(issueStatus), "result", "cancelled").increment());
    }

    /**
     * {@code issue.status=draft} porque el documento nunca llego a existir: el
     * rechazo ocurre antes de asignarle numero. Usar el estado que habria tenido
     * seria inventarse un hecho.
     */
    @Override
    public void documentRejected(SubscriptionBillingMetrics.Rejection rejection) {
        recorder.recordNow(() -> subscriptionDocuments
                .withTags("issue.status", "draft", "result", rejection.value()).increment());
    }

    @Override
    public void paymentRegistered(PaymentMethod method, SubscriptionPaymentStatus status) {
        recorder.recordAfterCommit(() -> subscriptionPayments
                .withTags("payment.method", lower(method), "result", paymentResult(status))
                .increment());
    }

    @Override
    public void paymentStatusChanged(PaymentMethod method, SubscriptionPaymentStatus status) {
        recorder.recordAfterCommit(() -> subscriptionPayments
                .withTags("payment.method", lower(method), "result", paymentResult(status))
                .increment());
    }

    @Override
    public void applicationRecorded(ApplicationSourceKind sourceKind) {
        recorder.recordAfterCommit(() -> subscriptionApplications
                .withTags("source.kind", lower(sourceKind), "result", "completed").increment());
    }

    @Override
    public void applicationReversed(ApplicationSourceKind sourceKind) {
        recorder.recordAfterCommit(() -> subscriptionApplications
                .withTags("source.kind", lower(sourceKind), "result", "cancelled").increment());
    }

    @Override
    public void statusTransitioned(SubscriptionStatus toStatus) {
        recorder.recordAfterCommit(() -> subscriptionStatusTransitions
                .withTags("to.status", lower(toStatus)).increment());
    }

    @Override
    public void recalculated(SubscriptionEntitlementMetrics.Trigger trigger) {
        recorder.recordAfterCommit(() -> subscriptionEntitlementRecalculations
                .withTags("trigger.reason", trigger.value(), "result", "completed").increment());
    }

    /**
     * {@code recordNow} y no {@code recordAfterCommit}: el recalculo corre DENTRO
     * de la transaccion del cambio de contrato, asi que su fallo la revierte y no
     * hay commit que esperar. Diferirlo seria descartar en silencio el unico
     * contador de la unica ruta que deja a una empresa entera sin permisos.
     */
    @Override
    public void recalculationFailed(SubscriptionEntitlementMetrics.Trigger trigger) {
        recorder.recordNow(() -> subscriptionEntitlementRecalculations
                .withTags("trigger.reason", trigger.value(), "result", "failed").increment());
    }

    /**
     * Traduce el estado alcanzado por el pago al vocabulario ya vivo del tag
     * {@code result}, en vez de abrir uno paralelo: {@code result=failed} tiene el
     * mismo significado aqui que en la entrega de documentos, y compartirlo es lo
     * que permite preguntar por todos los fallos del sistema de una vez.
     */
    private static String paymentResult(SubscriptionPaymentStatus status) {
        return switch (status) {
            case PENDING -> "pending";
            case CONFIRMED -> "completed";
            case FAILED -> "failed";
            case REFUNDED -> "cancelled";
        };
    }

    /** Ver el comentario de {@code charge.sign} en el filtro de cardinalidad. */
    private static String chargeSign(BigDecimal amount) {
        return amount.signum() < 0 ? "credit" : "debit";
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
