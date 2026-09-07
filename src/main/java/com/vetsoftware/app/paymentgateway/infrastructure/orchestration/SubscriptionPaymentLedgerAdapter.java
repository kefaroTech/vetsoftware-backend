package com.vetsoftware.app.paymentgateway.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.paymentgateway.domain.PaymentReservation;
import com.vetsoftware.app.paymentgateway.domain.PaymentReservationOutcome;
import com.vetsoftware.app.subscriptionpayment.application.command.ApplyBillingDocumentCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.AssignGatewayReferenceCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.ChangeSubscriptionPaymentStatusCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.RegisterSubscriptionPaymentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ApplyBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.AssignGatewayReferenceUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ChangeSubscriptionPaymentStatusUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.FindSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.RegisterSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.BillingDocumentApplicationJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Delega en {@code subscriptionpayment} el registro, la aplicación y el cierre
 * del pago del primer periodo.
 *
 * <p>
 * <strong>Con {@code SystemAuthRunner}</strong> para los tres casos de uso: los
 * tres están cerrados a {@code hasRole('SYSTEM')} a secas —cobrar la
 * suscripción es tesorería de la plataforma, ver el javadoc de
 * {@code RegisterSubscriptionPaymentUseCase}— y quien llega hasta aquí ya está
 * bajo esa escalada desde {@code ChargeContractFirstPeriodUseCase} o desde
 * {@code ProcessWompiEventUseCase} (webhook público, sin JWT).
 *
 * <p>
 * {@link #findDocumentIdByPayment} en cambio va directo al repositorio JPA de
 * {@code subscriptionpayment}: es una lectura, no requiere transacción propia
 * ni el gate de un caso de uso.
 */
@Component
public class SubscriptionPaymentLedgerAdapter implements SubscriptionPaymentLedgerPort {

    private final RegisterSubscriptionPaymentUseCase registerUseCase;
    private final ApplyBillingDocumentUseCase applyUseCase;
    private final AssignGatewayReferenceUseCase assignGatewayReferenceUseCase;
    private final ChangeSubscriptionPaymentStatusUseCase changeStatusUseCase;
    private final FindSubscriptionPaymentUseCase findUseCase;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final BillingDocumentApplicationJpaRepository billingDocumentApplicationJpaRepository;
    private final SystemAuthRunner systemAuthRunner;

    public SubscriptionPaymentLedgerAdapter(RegisterSubscriptionPaymentUseCase registerUseCase,
            ApplyBillingDocumentUseCase applyUseCase,
            AssignGatewayReferenceUseCase assignGatewayReferenceUseCase,
            ChangeSubscriptionPaymentStatusUseCase changeStatusUseCase,
            FindSubscriptionPaymentUseCase findUseCase,
            SubscriptionPaymentRepository subscriptionPaymentRepository,
            BillingDocumentApplicationJpaRepository billingDocumentApplicationJpaRepository,
            SystemAuthRunner systemAuthRunner) {
        this.registerUseCase = registerUseCase;
        this.applyUseCase = applyUseCase;
        this.assignGatewayReferenceUseCase = assignGatewayReferenceUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.findUseCase = findUseCase;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.billingDocumentApplicationJpaRepository = billingDocumentApplicationJpaRepository;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public Optional<PaymentReservation> findByClientRequestId(Long companyId,
            String clientRequestId) {
        return systemAuthRunner.call(() -> subscriptionPaymentRepository
                .findByCompanyIdAndClientRequestId(companyId, clientRequestId)
                .map(payment -> new PaymentReservation(payment.getId(),
                        payment.getGatewayReference())));
    }

    @Override
    public PaymentReservationOutcome registerAndApply(Long companyId, BigDecimal amount,
            String currency, String gatewayReference, LocalDateTime receivedAt,
            String clientRequestId, Long documentId) {
        return systemAuthRunner.call(() -> {
            PaymentReservationOutcome reservation = registerOrRecoverFromRace(companyId, amount,
                    currency, gatewayReference, receivedAt, clientRequestId);
            applyUseCase.execute(new ApplyBillingDocumentCommand(companyId, documentId,
                    ApplicationSourceKind.PAYMENT, reservation.paymentId(), null, amount,
                    clientRequestId));
            return reservation;
        });
    }

    /** Ver {@link SubscriptionPaymentLedgerPort#registerAndApply}. */
    private PaymentReservationOutcome registerOrRecoverFromRace(Long companyId, BigDecimal amount,
            String currency, String gatewayReference, LocalDateTime receivedAt,
            String clientRequestId) {
        try {
            SubscriptionPaymentDto payment = registerUseCase
                    .execute(new RegisterSubscriptionPaymentCommand(companyId, amount, currency,
                            PaymentMethod.CARD, PaymentGatewayNames.WOMPI, gatewayReference,
                            receivedAt, clientRequestId));
            return new PaymentReservationOutcome(payment.id(), false);
        } catch (DataIntegrityViolationException e) {
            Long recoveredId = subscriptionPaymentRepository
                    .findByCompanyIdAndClientRequestId(companyId, clientRequestId)
                    .map(SubscriptionPayment::getId).orElseThrow(() -> e);
            return new PaymentReservationOutcome(recoveredId, true);
        }
    }

    @Override
    public void assignGatewayReference(Long paymentId, Long companyId, String gatewayReference,
            LocalDateTime gatewayCreatedAt) {
        systemAuthRunner
                .run(() -> assignGatewayReferenceUseCase.execute(new AssignGatewayReferenceCommand(
                        paymentId, companyId, gatewayReference, gatewayCreatedAt)));
    }

    @Override
    public void confirm(Long paymentId, Long companyId) {
        systemAuthRunner
                .run(() -> changeStatusUseCase.execute(new ChangeSubscriptionPaymentStatusCommand(
                        paymentId, companyId, SubscriptionPaymentStatus.CONFIRMED)));
    }

    @Override
    public void fail(Long paymentId, Long companyId) {
        systemAuthRunner
                .run(() -> changeStatusUseCase.execute(new ChangeSubscriptionPaymentStatusCommand(
                        paymentId, companyId, SubscriptionPaymentStatus.FAILED)));
    }

    @Override
    public Optional<Long> findDocumentIdByPayment(Long companyId, Long paymentId) {
        return billingDocumentApplicationJpaRepository
                .findTargetDocumentIdsByPaymentId(paymentId, companyId).stream().findFirst();
    }

    @Override
    public Optional<FirstPeriodChargeOutcome> currentOutcome(Long paymentId, Long companyId) {
        return systemAuthRunner.call(() -> {
            SubscriptionPaymentDto payment = findUseCase.findById(paymentId, companyId);
            return switch (payment.status()) {
                case PENDING -> Optional.<FirstPeriodChargeOutcome>empty();
                case CONFIRMED -> Optional.of(FirstPeriodChargeOutcome.APPROVED);
                case FAILED, REFUNDED -> Optional.of(FirstPeriodChargeOutcome.DECLINED);
            };
        });
    }
}
