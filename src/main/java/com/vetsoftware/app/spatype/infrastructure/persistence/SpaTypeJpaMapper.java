package com.vetsoftware.app.spatype.infrastructure.persistence;

import com.vetsoftware.app.spatype.domain.SpaType;
import org.springframework.stereotype.Component;

@Component
public class SpaTypeJpaMapper {
    public SpaTypeJpaEntity toJpa(SpaType spaType) {
        SpaTypeJpaEntity entity = new SpaTypeJpaEntity();
        entity.setId(spaType.getId());
        entity.setName(spaType.getName());
        entity.setDescription(spaType.getDescription());
        entity.setCreatedDate(spaType.getCreatedDate());
        entity.setVersion(spaType.getVersion());
        entity.setEnabled(spaType.isEnabled());
        return entity;
    }

    public SpaType toDomain(SpaTypeJpaEntity entity) {
        return new SpaType(entity.getId(), entity.getName(), entity.getDescription(),
                entity.getCreatedDate(), entity.getVersion(), entity.isEnabled());
    }
}
