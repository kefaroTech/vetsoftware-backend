package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import org.springframework.stereotype.Component;

@Component
public class ConfiguratorEffectJpaMapper {

    public ConfiguratorEffectJpaEntity toJpa(ConfiguratorEffect effect) {
        ConfiguratorEffectJpaEntity entity = new ConfiguratorEffectJpaEntity();
        entity.setId(effect.getId());
        entity.setOptionId(effect.getOptionId());
        entity.setQuestionId(effect.getQuestionId());
        entity.setCatalogItemId(effect.getCatalogItemId());
        entity.setEffect(effect.getEffect());
        entity.setQuantity(effect.getQuantity());
        entity.setCreatedDate(effect.getCreatedDate());
        entity.setVersion(effect.getVersion());
        entity.setEnabled(effect.isEnabled());
        return entity;
    }

    public ConfiguratorEffect toDomain(ConfiguratorEffectJpaEntity entity) {
        return new ConfiguratorEffect(entity.getId(), entity.getOptionId(), entity.getQuestionId(),
                entity.getCatalogItemId(), entity.getEffect(), entity.getQuantity(),
                entity.getCreatedDate(), entity.getVersion(), entity.isEnabled());
    }
}
