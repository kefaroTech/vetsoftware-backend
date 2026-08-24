package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentRef;
import org.springframework.stereotype.Component;

@Component
public class BillingDocumentApplicationJpaMapper {

    public BillingDocumentApplicationJpaEntity toJpa(BillingDocumentApplication application,
            SubscriptionBillingDocumentJpaEntity targetDocument,
            SubscriptionPaymentJpaEntity payment,
            SubscriptionBillingDocumentJpaEntity sourceDocument,
            BillingDocumentApplicationJpaEntity reversalOf) {
        BillingDocumentApplicationJpaEntity entity = new BillingDocumentApplicationJpaEntity();
        entity.setId(application.getId());
        entity.setCompanyId(application.getCompanyId());
        entity.setTargetDocument(targetDocument);
        entity.setSourceKind(application.getSourceKind());
        entity.setPayment(payment);
        entity.setSourceDocument(sourceDocument);
        entity.setAppliedAmount(application.getAppliedAmount());
        entity.setReversalOf(reversalOf);
        entity.setClientRequestId(application.getClientRequestId());
        entity.setAppliedAt(application.getAppliedAt());
        entity.setCreatedDate(application.getCreatedDate());
        return entity;
    }

    /** Camino de lectura: el {@code @EntityGraph} ya hidrato las asociaciones. */
    public BillingDocumentApplication toDomain(BillingDocumentApplicationJpaEntity entity) {
        return toDomain(entity, SubscriptionBillingDocumentRefs.toRef(entity.getTargetDocument()),
                SubscriptionBillingDocumentRefs.toRef(entity.getSourceDocument()));
    }

    /**
     * Camino de escritura: reusa los {@code Ref} que el caso de uso ya resolvio, de
     * forma que el {@code getReferenceById} usado para escribir la FK no tenga que
     * hidratar su proxy solo para leerle el numero de documento.
     */
    public BillingDocumentApplication toDomain(BillingDocumentApplicationJpaEntity entity,
            BillingDocumentRef targetDocument, BillingDocumentRef sourceDocument) {
        return new BillingDocumentApplication(entity.getId(), entity.getCompanyId(), targetDocument,
                entity.getSourceKind(),
                entity.getPayment() == null ? null : entity.getPayment().getId(), sourceDocument,
                entity.getAppliedAmount(),
                entity.getReversalOf() == null ? null : entity.getReversalOf().getId(),
                entity.getClientRequestId(), entity.getAppliedAt(), entity.getCreatedDate());
    }
}
