package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.command.ChargeContractFirstPeriodCommand;
import com.vetsoftware.app.paymentgateway.application.dto.FirstPeriodChargeDto;
import com.vetsoftware.app.paymentgateway.application.dto.GatewayChargeResult;
import com.vetsoftware.app.paymentgateway.application.port.in.ChargeContractFirstPeriodUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.BillingDocumentIssuerPort;
import com.vetsoftware.app.paymentgateway.application.port.out.DefaultCardPaymentMethodQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.FirstPeriodPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptRecorderPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import com.vetsoftware.app.paymentgateway.domain.FiscalProfileNotConfiguredException;
import com.vetsoftware.app.paymentgateway.domain.GatewayDeclineKind;
import com.vetsoftware.app.paymentgateway.domain.IssuedPeriodDocument;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.paymentgateway.domain.PaymentMethodRef;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Cobra el primer periodo de un contrato recién firmado.
 *
 * <p>
 * <strong>Sin {@code @Transactional}</strong>: el tramo mecánico (correo,
 * transacción, sondeo, desenlace) vive en {@link GatewayCharger} y es I/O de
 * punta a punta; la regla dura {@code SIN_IO_EXTERNO_EN_TRANSACCION} sigue la
 * cadena de llamadas hasta ahí.
 *
 * <p>
 * <strong>Idempotente por la referencia del contrato</strong>
 * ({@code VS-<numero>-P1}), no por reintento de la transacción de Wompi: un
 * segundo disparo de {@code afterCommit} (o del propio webhook) encuentra el
 * pago ya registrado y traduce su estado en vez de volver a cobrar.
 *
 * <p>
 * <strong>Un primer rechazo SOFT no se reintenta por esta referencia.</strong>
 * {@code translateExisting} no oculta un {@code FAILED} —lo traduce a
 * {@link com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome#DECLINED},
 * nunca a {@code APPROVED}— así que un segundo disparo de {@code afterCommit}
 * no reintenta el cobro con {@code VS-<numero>-P1}. El reintento real pasa por
 * la escalera general de {@code payment_attempt}: {@link GatewayOutcomeSettler}
 * anota el intento contra el <em>mismo</em> {@code billingDocumentId} que este
 * servicio le pasó a {@link GatewayCharger} (el documento del periodo, no el
 * contrato), con {@code next_attempt_at} si el rechazo fue {@code SOFT};
 * {@code PaymentCollectionJob} lo recoge al día siguiente por
 * {@code findAllDueForRetry} y lo cobra con una referencia distinta
 * ({@code VS-DOC-<id>-A<n>}, vía {@link ChargeBillingDocumentService}). Un
 * rechazo {@code HARD} deja {@code next_attempt_at} nulo y sale de la cola
 * hasta que se fije un medio de pago nuevo.
 */
@Observed(name = "payment.gateway.charge.first.period")
@Service
public class ChargeContractFirstPeriodService implements ChargeContractFirstPeriodUseCase {

    private static final String REFERENCE_PREFIX = "VS-";
    private static final String REFERENCE_SUFFIX = "-P1";

    private final FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort;
    private final BillingDocumentIssuerPort billingDocumentIssuerPort;
    private final DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort;
    private final PaymentAttemptRecorderPort paymentAttemptRecorderPort;
    private final GatewayCharger gatewayCharger;
    private final ObservationRegistry observationRegistry;
    private final Clock clock;

    public ChargeContractFirstPeriodService(FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort,
            BillingDocumentIssuerPort billingDocumentIssuerPort,
            DefaultCardPaymentMethodQueryPort defaultCardPaymentMethodQueryPort,
            PaymentAttemptRecorderPort paymentAttemptRecorderPort, GatewayCharger gatewayCharger,
            ObservationRegistry observationRegistry, Clock clock) {
        this.firstPeriodPaymentQueryPort = firstPeriodPaymentQueryPort;
        this.billingDocumentIssuerPort = billingDocumentIssuerPort;
        this.defaultCardPaymentMethodQueryPort = defaultCardPaymentMethodQueryPort;
        this.paymentAttemptRecorderPort = paymentAttemptRecorderPort;
        this.gatewayCharger = gatewayCharger;
        this.observationRegistry = observationRegistry;
        this.clock = clock;
    }

    @Override
    public FirstPeriodChargeDto execute(ChargeContractFirstPeriodCommand command) {
        String reference = REFERENCE_PREFIX + command.subscriptionNumber() + REFERENCE_SUFFIX;

        var existing = firstPeriodPaymentQueryPort.findByCompanyIdAndReference(command.companyId(),
                reference);
        if (existing.isPresent()) {
            return tagged(translateExisting(existing.get()));
        }

        IssuedPeriodDocument document = billingDocumentIssuerPort.issue(command.companyId(),
                command.subscriptionId(), command.periodStart(), command.periodEnd());
        if (document.documentId() == null) {
            return tagged(new FirstPeriodChargeDto(FirstPeriodChargeOutcome.NOT_CONFIGURED, null,
                    "No hay cargos pendientes para el periodo"));
        }
        if (document.balanceAmount().signum() <= 0) {
            return tagged(new FirstPeriodChargeDto(FirstPeriodChargeOutcome.APPROVED, null, null));
        }

        PaymentMethodRef paymentMethod = defaultCardPaymentMethodQueryPort
                .findDefaultActiveCard(command.companyId(), PaymentGatewayNames.WOMPI).orElse(null);
        if (paymentMethod == null) {
            recordConfigurationAttempt(command.companyId(), document);
            return tagged(new FirstPeriodChargeDto(FirstPeriodChargeOutcome.NO_PAYMENT_METHOD, null,
                    "La empresa no tiene un medio de pago Wompi activo"));
        }

        try {
            GatewayChargeResult result = gatewayCharger.charge(command.companyId(),
                    document.documentId(), paymentMethod, document.balanceAmount(),
                    document.currency(), reference);
            return tagged(new FirstPeriodChargeDto(result.outcome(), result.gatewayReference(),
                    result.declineReason()));
        } catch (FiscalProfileNotConfiguredException e) {
            recordConfigurationAttempt(command.companyId(), document);
            return tagged(new FirstPeriodChargeDto(FirstPeriodChargeOutcome.NOT_CONFIGURED, null,
                    "La empresa no tiene perfil fiscal vigente"));
        }
    }

    /**
     * Un intento {@code CONFIGURATION} nunca deja {@code next_attempt_at} nulo; ver
     * {@code ChargeBillingDocumentService#recordConfigurationAttemptUnlessRecent}.
     */
    private void recordConfigurationAttempt(Long companyId, IssuedPeriodDocument document) {
        LocalDateTime now = LocalDateTime.now(clock);
        paymentAttemptRecorderPort.record(companyId, document.documentId(), null,
                PaymentGatewayNames.WOMPI, document.balanceAmount(), null,
                GatewayDeclineKind.CONFIGURATION, now, now.plusDays(1));
    }

    private FirstPeriodChargeDto translateExisting(FirstPeriodPaymentSnapshot snapshot) {
        FirstPeriodChargeOutcome outcome = switch (snapshot.status()) {
            case "CONFIRMED" -> FirstPeriodChargeOutcome.APPROVED;
            case "PENDING" -> FirstPeriodChargeOutcome.PENDING;
            default -> FirstPeriodChargeOutcome.DECLINED;
        };
        return new FirstPeriodChargeDto(outcome, snapshot.gatewayReference(), null);
    }

    private FirstPeriodChargeDto tagged(FirstPeriodChargeDto dto) {
        Observation current = observationRegistry.getCurrentObservation();
        if (current != null) {
            current.lowCardinalityKeyValue("payment.outcome",
                    dto.outcome().name().toLowerCase(Locale.ROOT));
            current.lowCardinalityKeyValue("payment.gateway", PaymentGatewayNames.WOMPI);
            if (dto.gatewayReference() != null) {
                current.highCardinalityKeyValue("gateway.reference", dto.gatewayReference());
            }
        }
        return dto;
    }
}
