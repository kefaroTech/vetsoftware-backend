package com.vetsoftware.app.smmlvvalue.infrastructure.persistence;

import com.vetsoftware.app.smmlvvalue.domain.SmmlvValue;
import org.springframework.stereotype.Component;

@Component
public class SmmlvValueJpaMapper {

    public SmmlvValueJpaEntity toJpa(SmmlvValue value) {
        SmmlvValueJpaEntity entity = new SmmlvValueJpaEntity();
        entity.setId(value.getId());
        entity.setFiscalYear((short) value.getFiscalYear());
        entity.setValueAmount(value.getValueAmount());
        entity.setLegalReference(value.getLegalReference());
        entity.setStatus(value.getStatus());
        entity.setStatusReference(value.getStatusReference());
        entity.setStatusChangedOn(value.getStatusChangedOn());
        entity.setCreatedDate(value.getCreatedDate());
        entity.setEnabled(value.isEnabled());
        entity.setVersion(value.getVersion());
        return entity;
    }

    public SmmlvValue toDomain(SmmlvValueJpaEntity entity) {
        return new SmmlvValue(entity.getId(), entity.getFiscalYear(), entity.getValueAmount(),
                entity.getLegalReference(), entity.getStatus(), entity.getStatusReference(),
                entity.getStatusChangedOn(), entity.getCreatedDate(), entity.isEnabled(),
                entity.getVersion());
    }
}
