package com.vetsoftware.app.module.infrastructure.persistence;

import com.vetsoftware.app.module.domain.Module;
import org.springframework.stereotype.Component;

@Component
public class ModuleJpaMapper {
    public ModuleJpaEntity toJpa(Module module) {
        ModuleJpaEntity entity = new ModuleJpaEntity();
        entity.setId(module.getId());
        entity.setName(module.getName());
        entity.setCode(module.getCode());
        entity.setCreatedDate(module.getCreatedDate());
        return entity;
    }

    public Module toDomain(ModuleJpaEntity entity) {
        return new Module(entity.getId(), entity.getName(), entity.getCode(), entity.getCreatedDate());
    }
}
