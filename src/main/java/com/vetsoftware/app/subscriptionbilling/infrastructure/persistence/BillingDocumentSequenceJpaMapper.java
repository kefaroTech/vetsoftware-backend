package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequence;
import org.springframework.stereotype.Component;

/** Ida y vuelta entre la serie de dominio y su fila. */
@Component
public class BillingDocumentSequenceJpaMapper {

    public BillingDocumentSequenceJpaEntity toJpa(BillingDocumentSequence sequence) {
        BillingDocumentSequenceJpaEntity entity = new BillingDocumentSequenceJpaEntity();
        entity.setId(sequence.getId());
        entity.setPrefix(sequence.getPrefix());
        entity.setNextValue(sequence.getNextValue());
        entity.setCreatedDate(sequence.getCreatedDate());
        return entity;
    }

    public BillingDocumentSequence toDomain(BillingDocumentSequenceJpaEntity entity) {
        return new BillingDocumentSequence(entity.getId(), entity.getPrefix(),
                entity.getNextValue(), entity.getCreatedDate());
    }
}
