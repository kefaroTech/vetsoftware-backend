package com.vetsoftware.app.basepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.domain.BasePermission;
import com.vetsoftware.app.basepermission.domain.SubModuleRef;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class BasePermissionJpaMapper {

    public BasePermissionJpaEntity toJpa(BasePermission basePermission,
            SubModuleJpaEntity subModule) {
        BasePermissionJpaEntity entity = new BasePermissionJpaEntity();
        entity.setId(basePermission.getId());
        entity.setName(basePermission.getName());
        entity.setCode(basePermission.getCode());
        entity.setSubModule(subModule);
        entity.setCreatedDate(basePermission.getCreatedDate());
        entity.setEnabled(basePermission.isEnabled());
        return entity;
    }

    public BasePermission toDomain(BasePermissionJpaEntity entity) {
        SubModuleJpaEntity sm = entity.getSubModule();
        return toDomain(entity, new SubModuleRef(sm.getId(), sm.getName(), sm.getCode()));
    }

    public BasePermission toDomain(BasePermissionJpaEntity entity, SubModuleRef subModuleRef) {
        return new BasePermission(entity.getId(), entity.getName(), entity.getCode(), subModuleRef,
                entity.getCreatedDate(), entity.isEnabled());
    }
}
