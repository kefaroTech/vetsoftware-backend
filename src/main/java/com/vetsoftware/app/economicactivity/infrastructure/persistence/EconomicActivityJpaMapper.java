package com.vetsoftware.app.economicactivity.infrastructure.persistence;

import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import org.springframework.stereotype.Component;

@Component
public class EconomicActivityJpaMapper {
    public EconomicActivityJpaEntity toJpa(EconomicActivity economicActivity) {
        EconomicActivityJpaEntity entity = new EconomicActivityJpaEntity();
        entity.setId(economicActivity.getId());
        entity.setCode(economicActivity.getCode());
        entity.setName(economicActivity.getName());
        entity.setCreatedDate(economicActivity.getCreatedDate());
        entity.setEnabled(economicActivity.isEnabled());
        return entity;
    }

    public EconomicActivity toDomain(EconomicActivityJpaEntity entity) {
        return new EconomicActivity(entity.getId(), entity.getCode(), entity.getName(),
                entity.getCreatedDate(), entity.isEnabled());
    }
}
