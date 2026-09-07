package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import com.vetsoftware.app.paymentgateway.application.command.ProcessWompiEventCommand;
import com.vetsoftware.app.paymentgateway.application.port.in.ProcessWompiEventUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.FirstPeriodPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.GatewayWebhookEventRecorderPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentGatewayMetrics;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.application.port.out.WompiEventPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.GatewayWebhookOutcome;
import com.vetsoftware.app.paymentgateway.domain.ParsedWompiEvent;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.paymentgateway.domain.WompiChecksumMismatchException;
import com.vetsoftware.app.paymentgateway.domain.WompiStaleEventException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Procesa un webhook de Wompi.
 *
 * <p>
 * <strong>Sin {@code @Transactional}</strong>: delega en
 * {@code SubscriptionPaymentLedgerPort} y {@code PaymentAttemptRecorderPort} (a
 * través de {@link GatewayOutcomeSettler}), que llaman a casos de uso
 * {@code SYSTEM} de otras rodajas y cada uno abre su propia transacción.
 *
 * <p>
 * <strong>Orden de validación: parsear, exigir configuración, frescura,
 * checksum, persistir, y solo entonces el resto</strong>; ver
 * {@link GatewayWebhookOutcome}.
 *
 * <p>
 * <strong>Checksum inválido o evento sin frescura lanzan, no retornan.</strong>
 * Un webhook forjado o repetido fuera de ventana que recibiera 200 quedaría
 * indistinguible de uno auténtico para el reintento de Wompi, que no reintenta
 * un 200. {@link WompiChecksumMismatchException} y
 * {@link WompiStaleEventException} se mapean a 401.
 */
@Observed(name = "payment.gateway.event.process")
@Service
public class ProcessWompiEventService implements ProcessWompiEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessWompiEventService.class);

    private static final String PENDING = "PENDING";

    /**
     * El webhook no cruza el borde de {@code AuthFilter} (ruta pública): sin este
     * valor, los cambios de estado que dispara aquí quedan sin actor en el canal
     * AUDIT.
     */
    private static final String GATEWAY_ACTOR = "GATEWAY";

    private final WompiEventPort wompiEventPort;
    private final FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort;
    private final DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort;
    private final SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort;
    private final GatewayOutcomeSettler outcomeSettler;
    private final GatewayWebhookEventRecorderPort webhookEventRecorderPort;
    private final PaymentGatewayMetrics metrics;
    private final ObservationRegistry observationRegistry;
    private final Clock clock;

    public ProcessWompiEventService(WompiEventPort wompiEventPort,
            FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort,
            DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort,
            SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort,
            GatewayOutcomeSettler outcomeSettler,
            GatewayWebhookEventRecorderPort webhookEventRecorderPort, PaymentGatewayMetrics metrics,
            ObservationRegistry observationRegistry, Clock clock) {
        this.wompiEventPort = wompiEventPort;
        this.firstPeriodPaymentQueryPort = firstPeriodPaymentQueryPort;
        this.defaultCardPaymentMethodQueryPort = defaultCardPaymentMethodQueryPort;
        this.subscriptionPaymentLedgerPort = subscriptionPaymentLedgerPort;
        this.outcomeSettler = outcomeSettler;
        this.webhookEventRecorderPort = webhookEventRecorderPort;
        this.metrics = metrics;
        this.observationRegistry = observationRegistry;
        this.clock = clock;
    }

    @Override
    public void execute(ProcessWompiEventCommand command) {
        ParsedWompiEvent event = wompiEventPort.parse(command.rawBody());
        tagObservation(event.transactionId());
        wompiEventPort.requireConfigured();

        boolean fresh = isFresh(event);
        if (!wompiEventPort.matchesChecksum(event, command.checksumHeader())) {
            recordUnauthenticatedOutcome(GatewayWebhookOutcome.REJECTED_CHECKSUM);
            throw new WompiChecksumMismatchException(
                    "El checksum del webhook de Wompi no coincide");
        }

        String checksum = wompiEventPort.computeChecksum(event);
        if (webhookEventRecorderPort.existsByChecksum(PaymentGatewayNames.WOMPI, checksum)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Long eventId = webhookEventRecorderPort.recordReceived(PaymentGatewayNames.WOMPI,
                event.eventType(), checksum, event.transactionId(), now, command.rawBody());

        if (!fresh) {
            recordOutcome(eventId, now, GatewayWebhookOutcome.REJECTED_STALE);
            throw new WompiStaleEventException(
                    "El webhook de Wompi llegó fuera de la ventana de frescura permitida");
        }
        if (!event.isTransactionUpdated()) {
            recordOutcome(eventId, now, GatewayWebhookOutcome.IGNORED_UNKNOWN_EVENT);
            return;
        }

        FirstPeriodPaymentSnapshot snapshot = firstPeriodPaymentQueryPort
                .findByGatewayAndReference(PaymentGatewayNames.WOMPI, event.transactionId())
                .orElse(null);
        if (snapshot == null) {
            log.warn("Webhook de Wompi para una transacción sin pago conocido: {}",
                    event.transactionId());
            recordOutcome(eventId, now, GatewayWebhookOutcome.PAYMENT_NOT_FOUND);
            return;
        }
        if (!PENDING.equals(snapshot.status())) {
            recordOutcome(eventId, now, GatewayWebhookOutcome.IGNORED_ALREADY_FINAL);
            return;
        }

        String previousActor = MDC.get(MdcKeys.ACTOR_TYPE);
        String previousCompany = MDC.get(MdcKeys.ACTOR_COMPANY_ID);
        MDC.put(MdcKeys.ACTOR_TYPE, GATEWAY_ACTOR);
        MDC.put(MdcKeys.ACTOR_COMPANY_ID, String.valueOf(snapshot.companyId()));
        try {
            if (event.status() == GatewayTransactionStatus.APPROVED
                    && !amountMatches(event.amountInCents(), snapshot.amount())) {
                log.warn(
                        "Webhook de Wompi APPROVED con importe distinto al registrado:"
                                + " esperado={} centavos, recibido={} centavos",
                        GatewayCharger.toAmountInCents(snapshot.amount()), event.amountInCents());
                recordOutcome(eventId, now, GatewayWebhookOutcome.REJECTED_AMOUNT);
                return;
            }

            Long paymentMethodId = defaultCardPaymentMethodQueryPort
                    .findDefaultActiveCard(snapshot.companyId(), PaymentGatewayNames.WOMPI)
                    .map(ref -> ref.id()).orElse(null);
            Long documentId = subscriptionPaymentLedgerPort
                    .findDocumentIdByPayment(snapshot.companyId(), snapshot.paymentId())
                    .orElse(null);

            outcomeSettler.settle(event.status(), event.statusMessage(), snapshot.companyId(),
                    snapshot.paymentId(), documentId, paymentMethodId, snapshot.amount());
            recordOutcome(eventId, now, GatewayWebhookOutcome.APPLIED);
        } finally {
            restore(MdcKeys.ACTOR_TYPE, previousActor);
            restore(MdcKeys.ACTOR_COMPANY_ID, previousCompany);
        }
    }

    /**
     * Único punto por el que sale un desenlace persistido del webhook: escribe en
     * {@code gateway_webhook_events} (fuente de verdad), lo cuenta y lo deja en el
     * span. Un desenlace que pasara por fuera de aquí quedaría contado o trazado a
     * medias.
     */
    private void recordOutcome(Long eventId, LocalDateTime now, GatewayWebhookOutcome outcome) {
        webhookEventRecorderPort.recordOutcome(eventId, now, outcome);
        tagOutcome(outcome);
    }

    /**
     * Un checksum inválido no deja fila que actualizar: solo el contador y el
     * atributo del span, sin tocar {@code gateway_webhook_events}.
     */
    private void recordUnauthenticatedOutcome(GatewayWebhookOutcome outcome) {
        tagOutcome(outcome);
    }

    private void tagOutcome(GatewayWebhookOutcome outcome) {
        metrics.recordWebhookOutcome(outcome);
        Observation current = observationRegistry.getCurrentObservation();
        if (current != null) {
            current.lowCardinalityKeyValue("payment.outcome",
                    outcome.name().toLowerCase(Locale.ROOT));
        }
    }

    private void tagObservation(String gatewayReference) {
        Observation current = observationRegistry.getCurrentObservation();
        if (current == null) {
            return;
        }
        current.lowCardinalityKeyValue("payment.gateway", PaymentGatewayNames.WOMPI);
        current.highCardinalityKeyValue("gateway.reference", gatewayReference);
    }

    private boolean isFresh(ParsedWompiEvent event) {
        Instant eventInstant = Instant.ofEpochSecond(event.timestamp());
        Duration age = Duration.between(eventInstant, Instant.now(clock)).abs();
        return age.compareTo(wompiEventPort.freshnessTolerance()) <= 0;
    }

    private static boolean amountMatches(long amountInCents, BigDecimal registeredAmount) {
        return amountInCents == GatewayCharger.toAmountInCents(registeredAmount);
    }

    private static void restore(String key, String previous) {
        if (previous == null) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, previous);
    }
}
