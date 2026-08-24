package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("subscriptionPaymentJpaBillingDocumentQueryPort")
public class JpaBillingDocumentQueryPort implements BillingDocumentQueryPort {

    private final SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository;

    public JpaBillingDocumentQueryPort(
            SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository) {
        this.billingDocumentJpaRepository = billingDocumentJpaRepository;
    }

    @Override
    public Optional<BillingDocumentRef> findByIdAndCompanyId(Long documentId, Long companyId) {
        return billingDocumentJpaRepository.findByIdAndCompanyId(documentId, companyId)
                .map(SubscriptionBillingDocumentRefs::toRef);
    }

    @Override
    public void lockByIdAndCompanyId(Long documentId, Long companyId) {
        // Sin resultado a proposito: el objetivo es el candado, no la fila. Si el
        // documento no es de esta empresa no devuelve nada y no se bloquea nada; la
        // resolucion acotada posterior es la que reporta el error.
        billingDocumentJpaRepository.lockByIdAndCompanyId(documentId, companyId);
    }
}
