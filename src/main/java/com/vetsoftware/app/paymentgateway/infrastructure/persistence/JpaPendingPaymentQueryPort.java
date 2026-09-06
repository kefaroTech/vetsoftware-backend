package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.paymentgateway.application.port.out.PendingPaymentQueryPort;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import com.vetsoftware.app.subscriptionpayment.infrastructure.persistence.BillingDocumentApplicationJpaRepository;
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

    public JpaPendingPaymentQueryPort(BillingDocumentApplicationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsPendingPayment(Long companyId, Long billingDocumentId) {
        return repository
                .findAllByTargetDocument_IdAndCompanyId(billingDocumentId, companyId,
                        Pageable.unpaged())
                .stream().anyMatch(
                        application -> application.getSourceKind() == ApplicationSourceKind.PAYMENT
                                && application.getPayment() != null && application.getPayment()
                                        .getStatus() == SubscriptionPaymentStatus.PENDING);
    }
}
