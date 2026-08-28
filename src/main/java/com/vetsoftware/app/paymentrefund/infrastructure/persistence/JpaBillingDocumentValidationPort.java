package com.vetsoftware.app.paymentrefund.infrastructure.persistence;

import com.vetsoftware.app.paymentrefund.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code subscriptionbilling}.
 *
 * <p>
 * Se apoya en {@code findByIdAndCompanyId} y no lee un solo getter de la
 * entidad ajena: al configurador de esta devolucion no le hace falta ningun
 * campo del documento, y depender de la forma de una entidad de otra feature es
 * como un cambio inocente alli rompe esto.
 */
@Component("paymentRefundJpaBillingDocumentValidationPort")
public class JpaBillingDocumentValidationPort implements BillingDocumentValidationPort {

    private final SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository;

    public JpaBillingDocumentValidationPort(
            SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository) {
        this.billingDocumentJpaRepository = billingDocumentJpaRepository;
    }

    @Override
    public boolean existsByIdAndCompanyId(Long documentId, Long companyId) {
        return documentId != null && companyId != null && billingDocumentJpaRepository
                .findByIdAndCompanyId(documentId, companyId).isPresent();
    }
}
