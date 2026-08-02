package com.vetsoftware.app.submodule.infrastructure.persistence;

import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaEntity;
import com.vetsoftware.app.submodule.domain.ModuleRef;
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
        entity.setEnabled(subModule.isEnabled());
        return entity;
    }

    public SubModule toDomain(SubModuleJpaEntity entity) {
        ModuleJpaEntity m = entity.getModule();
        return toDomain(entity, new ModuleRef(m.getId(), m.getName(), m.getCode()));
    }

    public SubModule toDomain(SubModuleJpaEntity entity, ModuleRef ref) {
        return new SubModule(entity.getId(), entity.getName(), entity.getCode(), ref,
                entity.getCreatedDate(), entity.isEnabled());
    }
}
