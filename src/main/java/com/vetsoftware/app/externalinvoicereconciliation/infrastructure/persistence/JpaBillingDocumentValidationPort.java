package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code subscriptionbilling}.
 *
 * <p>
 * Se apoya en {@code findByIdAndCompanyId} y <strong>no lee un solo
 * getter</strong> de la entidad ajena: aqui no hace falta ningun campo del
 * documento, y depender de la forma de una entidad de otra feature es como un
 * cambio inocente alli rompe esto.
 *
 * <p>
 * El nombre del bean va cualificado porque el vertical slicing repite el nombre
 * simple: {@code paymentrefund} declara su propio
 * {@code JpaBillingDocumentValidationPort}, y sin cualificar los dos pelearian
 * por el mismo nombre de bean.
 */
@Component("externalInvoiceReconciliationJpaBillingDocumentValidationPort")
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
