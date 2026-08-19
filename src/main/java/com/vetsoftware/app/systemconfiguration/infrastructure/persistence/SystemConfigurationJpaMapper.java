package com.vetsoftware.app.systemconfiguration.infrastructure.persistence;

import com.vetsoftware.app.systemconfiguration.domain.SystemConfiguration;
import org.springframework.stereotype.Component;

@Component
public class SystemConfigurationJpaMapper {

    public SystemConfigurationJpaEntity toJpa(SystemConfiguration config) {
        SystemConfigurationJpaEntity entity = new SystemConfigurationJpaEntity();
        entity.setId(config.getId());
        entity.setPropertyName(config.getPropertyName());
        entity.setValue(config.getValue());
        entity.setCreatedDate(config.getCreatedDate());
        entity.setVersion(config.getVersion());
        entity.setEnabled(config.isEnabled());
        return entity;
    }

    public SystemConfiguration toDomain(SystemConfigurationJpaEntity entity) {
        return new SystemConfiguration(entity.getId(), entity.getPropertyName(), entity.getValue(),
                entity.getCreatedDate(), entity.getVersion(), entity.isEnabled());
    }
}
