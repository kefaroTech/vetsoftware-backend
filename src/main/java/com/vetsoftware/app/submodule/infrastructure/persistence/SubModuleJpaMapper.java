package com.vetsoftware.app.submodule.infrastructure.persistence;

import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaEntity;
import com.vetsoftware.app.submodule.domain.SubModule;
import org.springframework.stereotype.Component;

@Component
public class SubModuleJpaMapper {

    public SubModuleJpaEntity toJpa(SubModule subModule, ModuleJpaEntity module) {
        SubModuleJpaEntity entity = new SubModuleJpaEntity();
        entity.setId(subModule.getId());
        entity.setName(subModule.getName());
        entity.setCode(subModule.getCode());
        entity.setModule(module);
        entity.setCreatedDate(subModule.getCreatedDate());
        return entity;
    }

    public SubModule toDomain(SubModuleJpaEntity entity) {
        return new SubModule(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            entity.getModule().getId(),
            entity.getCreatedDate()
        );
    }
}
