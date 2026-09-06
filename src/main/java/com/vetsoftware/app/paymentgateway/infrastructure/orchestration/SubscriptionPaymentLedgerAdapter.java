package com.vetsoftware.app.paymentgateway.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentgateway.application.port.out.SubscriptionPaymentLedgerPort;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNames;
import com.vetsoftware.app.subscriptionpayment.application.command.ApplyBillingDocumentCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.ChangeSubscriptionPaymentStatusCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.RegisterSubscriptionPaymentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ApplyBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ChangeSubscriptionPaymentStatusUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.in.RegisterSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.BillingDocumentApplicationJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
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
    private final ChangeSubscriptionPaymentStatusUseCase changeStatusUseCase;
    private final BillingDocumentApplicationJpaRepository billingDocumentApplicationJpaRepository;
    private final SystemAuthRunner systemAuthRunner;

    public SubscriptionPaymentLedgerAdapter(RegisterSubscriptionPaymentUseCase registerUseCase,
            ApplyBillingDocumentUseCase applyUseCase,
            ChangeSubscriptionPaymentStatusUseCase changeStatusUseCase,
            BillingDocumentApplicationJpaRepository billingDocumentApplicationJpaRepository,
            SystemAuthRunner systemAuthRunner) {
        this.registerUseCase = registerUseCase;
        this.applyUseCase = applyUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.billingDocumentApplicationJpaRepository = billingDocumentApplicationJpaRepository;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public Long registerAndApply(Long companyId, BigDecimal amount, String currency,
            String gatewayReference, LocalDateTime receivedAt, String clientRequestId,
            Long documentId) {
        return systemAuthRunner.call(() -> {
            SubscriptionPaymentDto payment = registerUseCase
                    .execute(new RegisterSubscriptionPaymentCommand(companyId, amount, currency,
                            PaymentMethod.CARD, PaymentGatewayNames.WOMPI, gatewayReference,
                            receivedAt, clientRequestId));
            applyUseCase.execute(new ApplyBillingDocumentCommand(companyId, documentId,
                    ApplicationSourceKind.PAYMENT, payment.id(), null, amount, clientRequestId));
            return payment.id();
        });
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
}
