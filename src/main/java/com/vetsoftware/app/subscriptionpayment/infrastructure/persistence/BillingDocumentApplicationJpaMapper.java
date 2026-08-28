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
        // Los dos origenes de la capa K que si apuntan a una fila. Van como escalares
        // porque su tabla es de otro slice y esta rodaja no navega a ella; la FK
        // compuesta de la base sigue siendo quien rechaza una referencia ajena.
        entity.setWithholdingId(application.getWithholdingId());
        entity.setCreditEntryId(application.getCreditEntryId());
        entity.setAppliedAmount(application.getAppliedAmount());
        entity.setReversalOf(reversalOf);
        entity.setWriteOffAuthorizedBySystemUserId(
                application.getWriteOffAuthorizedBySystemUserId());
        entity.setWriteOffReason(application.getWriteOffReason());
        entity.setClientRequestId(application.getClientRequestId());
        entity.setAppliedAt(application.getAppliedAt());
        // Ya no se deriva de appliedAt: el dominio la modela y la exige. La derivacion
        // valia mientras los unicos origenes escribibles se aplicaban el dia que
        // ocurrian; una retencion practicada el 30 de octubre y registrada el 3 de
        // noviembre pertenece a octubre, y derivarla la habria puesto en noviembre.
        entity.setValueDate(application.getValueDate());
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
                entity.getWithholdingId(), entity.getCreditEntryId(), entity.getAppliedAmount(),
                entity.getReversalOf() == null ? null : entity.getReversalOf().getId(),
                entity.getWriteOffAuthorizedBySystemUserId(), entity.getWriteOffReason(),
                entity.getClientRequestId(), entity.getAppliedAt(), entity.getValueDate(),
                entity.getCreatedDate());
    }
}
