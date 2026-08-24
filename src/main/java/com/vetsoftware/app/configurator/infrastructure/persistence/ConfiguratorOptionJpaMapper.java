package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import org.springframework.stereotype.Component;

@Component
public class ConfiguratorOptionJpaMapper {

    public ConfiguratorOptionJpaEntity toJpa(ConfiguratorOption option) {
        ConfiguratorOptionJpaEntity entity = new ConfiguratorOptionJpaEntity();
        entity.setId(option.getId());
        entity.setQuestionId(option.getQuestionId());
        entity.setCode(option.getCode());
        entity.setLabel(option.getLabel());
        entity.setHelpText(option.getHelpText());
        entity.setSortOrder(option.getSortOrder());
        entity.setCreatedDate(option.getCreatedDate());
        entity.setVersion(option.getVersion());
        entity.setEnabled(option.isEnabled());
        return entity;
    }

    public ConfiguratorOption toDomain(ConfiguratorOptionJpaEntity entity) {
        return new ConfiguratorOption(entity.getId(), entity.getQuestionId(), entity.getCode(),
                entity.getLabel(), entity.getHelpText(), entity.getSortOrder(),
                entity.getCreatedDate(), entity.getVersion(), entity.isEnabled());
    }
}
