package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.paymentgateway.application.port.out.PaymentAttemptQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.PendingPaymentQueryPort;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.BillingDocumentApplicationJpaRepository;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Consume {@code BillingDocumentApplicationJpaRepository} de
 * {@code subscriptionpayment} por su variante paginada de aplicaciones del
 * documento, sin importar su dominio.
 *
 * <p>
 * <strong>Sin un {@code existsByTargetDocumentAndPendingPayment}
 * dedicado.</strong> Este slice no toca ficheros de
 * {@code subscriptionpayment}, así que filtra en memoria sobre la única
 * variante que ya expone el repositorio: a lo sumo un puñado de aplicaciones
 * por documento.
 */
@Component
public class JpaPendingPaymentQueryPort implements PendingPaymentQueryPort {

    private final BillingDocumentApplicationJpaRepository repository;
    private final PaymentAttemptQueryPort paymentAttemptQueryPort;

    public JpaPendingPaymentQueryPort(BillingDocumentApplicationJpaRepository repository,
            PaymentAttemptQueryPort paymentAttemptQueryPort) {
        this.repository = repository;
        this.paymentAttemptQueryPort = paymentAttemptQueryPort;
    }

    @Override
    public boolean existsPendingPayment(Long companyId, Long billingDocumentId) {
        return repository
                .findAllByTargetDocument_IdAndCompanyId(billingDocumentId, companyId,
                        Pageable.unpaged())
                .stream()
                .filter(application -> application.getSourceKind() == ApplicationSourceKind.PAYMENT
                        && application.getPayment() != null
                        && application.getPayment()
                                .getStatus() == SubscriptionPaymentStatus.PENDING)
                .anyMatch(application -> !isStale(companyId, billingDocumentId,
                        application.getPayment().getReceivedAt()));
    }

    /**
     * {@code ReconcilePendingPaymentsService} anota un intento {@code SOFT}
     * posterior a la recepción del pago cuando Wompi lo sostiene {@code PENDING}
     * más allá del umbral, en vez de marcarlo {@code FAILED}. Ese intento es la
     * señal de que el pago ya no debe bloquear el recobro, aunque su fila siga
     * {@code PENDING}.
     */
    private boolean isStale(Long companyId, Long billingDocumentId, LocalDateTime receivedAt) {
        return paymentAttemptQueryPort.findLast(companyId, billingDocumentId)
                .map(attempt -> !attempt.attemptedAt().isBefore(receivedAt)).orElse(false);
    }
}
