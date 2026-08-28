package com.vetsoftware.app.customercredit.infrastructure.persistence;

import com.vetsoftware.app.customercredit.domain.CustomerCreditEntry;
import org.springframework.stereotype.Component;

@Component
public class CustomerCreditEntryJpaMapper {

    public CustomerCreditEntryJpaEntity toJpa(CustomerCreditEntry entry) {
        CustomerCreditEntryJpaEntity entity = new CustomerCreditEntryJpaEntity();
        entity.setId(entry.getId());
        entity.setCompanyId(entry.getCompanyId());
        entity.setEntryKind(entry.getEntryKind());
        entity.setAmount(entry.getAmount());
        entity.setLotEntryId(entry.getLotEntryId());
        entity.setOriginKind(entry.getOriginKind());
        entity.setOriginPaymentId(entry.getOriginPaymentId());
        entity.setOriginDocumentId(entry.getOriginDocumentId());
        entity.setOriginSubscriptionId(entry.getOriginSubscriptionId());
        entity.setOccurredAt(entry.getOccurredAt());
        entity.setValueDate(entry.getValueDate());
        entity.setExpiresOn(entry.getExpiresOn());
        entity.setClientRequestId(entry.getClientRequestId());
        entity.setCreatedDate(entry.getCreatedDate());
        return entity;
    }

    public CustomerCreditEntry toDomain(CustomerCreditEntryJpaEntity entity) {
        return new CustomerCreditEntry(entity.getId(), entity.getCompanyId(), entity.getEntryKind(),
                entity.getAmount(), entity.getLotEntryId(), entity.getOriginKind(),
                entity.getOriginPaymentId(), entity.getOriginDocumentId(),
                entity.getOriginSubscriptionId(), entity.getOccurredAt(), entity.getValueDate(),
                entity.getExpiresOn(), entity.getClientRequestId(), entity.getCreatedDate());
    }
}
