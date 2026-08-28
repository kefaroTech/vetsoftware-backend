package com.vetsoftware.app.uvtvalue.infrastructure.persistence;

import com.vetsoftware.app.uvtvalue.domain.UvtValue;
import org.springframework.stereotype.Component;

@Component
public class UvtValueJpaMapper {

    public UvtValueJpaEntity toJpa(UvtValue value) {
        UvtValueJpaEntity entity = new UvtValueJpaEntity();
        entity.setId(value.getId());
        entity.setFiscalYear((short) value.getFiscalYear());
        entity.setValueAmount(value.getValueAmount());
        entity.setLegalReference(value.getLegalReference());
        entity.setCreatedDate(value.getCreatedDate());
        entity.setEnabled(value.isEnabled());
        return entity;
    }

    public UvtValue toDomain(UvtValueJpaEntity entity) {
        return new UvtValue(entity.getId(), entity.getFiscalYear(), entity.getValueAmount(),
                entity.getLegalReference(), entity.getCreatedDate(), entity.isEnabled());
    }
}
