package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.command.ProcessWompiEventCommand;
import com.vetsoftware.app.paymentgateway.application.port.in.ProcessWompiEventUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.FirstPeriodPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.application.port.out.WompiEventPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import com.vetsoftware.app.paymentgateway.domain.ParsedWompiEvent;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.paymentgateway.domain.WompiChecksumMismatchException;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <strong>Checksum inválido lanza, no retorna.</strong> Un webhook forjado que
 * recibiera 200 quedaría indistinguible de uno auténtico para el reintento de
 * Wompi —que no reintenta un 200—, y el atacante lo sabría por la propia
 * respuesta. {@link WompiChecksumMismatchException} se mapea a 401.
 */
@Observed(name = "payment.gateway.event.process")
@Service
public class ProcessWompiEventService implements ProcessWompiEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessWompiEventService.class);

    private static final String PENDING = "PENDING";

    private final WompiEventPort wompiEventPort;
    private final FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort;
    private final DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort;
    private final SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort;
    private final GatewayOutcomeSettler outcomeSettler;

    public ProcessWompiEventService(WompiEventPort wompiEventPort,
            FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort,
            DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort,
            SubscriptionPaymentLedgerPort subscriptionPaymentLedgerPort,
            GatewayOutcomeSettler outcomeSettler) {
        this.wompiEventPort = wompiEventPort;
        this.firstPeriodPaymentQueryPort = firstPeriodPaymentQueryPort;
        this.defaultCardPaymentMethodQueryPort = defaultCardPaymentMethodQueryPort;
        this.subscriptionPaymentLedgerPort = subscriptionPaymentLedgerPort;
        this.outcomeSettler = outcomeSettler;
    }

    @Override
    public void execute(ProcessWompiEventCommand command) {
        ParsedWompiEvent event = wompiEventPort.parse(command.rawBody());
        if (!wompiEventPort.matchesChecksum(event, command.checksumHeader())) {
            throw new WompiChecksumMismatchException(
                    "El checksum del webhook de Wompi no coincide");
        }
        if (!event.isTransactionUpdated()) {
            return;
        }

        FirstPeriodPaymentSnapshot snapshot = firstPeriodPaymentQueryPort
                .findByGatewayAndReference(PaymentGatewayNames.WOMPI, event.transactionId())
                .orElse(null);
        if (snapshot == null) {
            log.warn("Webhook de Wompi para una transacción sin pago conocido: {}",
                    event.transactionId());
            return;
        }
        if (!PENDING.equals(snapshot.status())) {
            // Ya está en un estado final: un reintento del webhook no vuelve a tocar nada.
            return;
        }

        Long paymentMethodId = defaultCardPaymentMethodQueryPort
                .findDefaultActiveCard(snapshot.companyId(), PaymentGatewayNames.WOMPI)
                .map(ref -> ref.id()).orElse(null);
        Long documentId = subscriptionPaymentLedgerPort
                .findDocumentIdByPayment(snapshot.companyId(), snapshot.paymentId()).orElse(null);

        outcomeSettler.settle(event.status(), event.statusMessage(), snapshot.companyId(),
                snapshot.paymentId(), documentId, paymentMethodId, snapshot.amount());
    }
}
