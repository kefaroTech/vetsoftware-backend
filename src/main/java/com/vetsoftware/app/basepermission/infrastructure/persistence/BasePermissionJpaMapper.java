package com.vetsoftware.app.basepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.domain.BasePermission;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class BasePermissionJpaMapper {

    public BasePermissionJpaEntity toJpa(BasePermission basePermission, SubModuleJpaEntity subModule) {
        BasePermissionJpaEntity entity = new BasePermissionJpaEntity();
        entity.setId(basePermission.getId());
        entity.setName(basePermission.getName());
        entity.setCode(basePermission.getCode());
        entity.setSubModule(subModule);
        entity.setCreatedDate(basePermission.getCreatedDate());
        return entity;
    }

    public BasePermission toDomain(BasePermissionJpaEntity entity) {
        return new BasePermission(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            entity.getSubModule().getId(),
            entity.getCreatedDate()
        );
    }
}
