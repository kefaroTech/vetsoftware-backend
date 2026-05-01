package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaEntity;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import org.springframework.stereotype.Component;

@Component
public class BaseRolePermissionJpaMapper {

    public BaseRolePermissionJpaEntity toJpa(BaseRolePermission baseRolePermission,
                                              BaseRoleJpaEntity baseRole,
                                              BasePermissionJpaEntity basePermission) {
        BaseRolePermissionJpaEntity entity = new BaseRolePermissionJpaEntity();
        entity.setId(baseRolePermission.getId());
        entity.setBaseRole(baseRole);
        entity.setBasePermission(basePermission);
        entity.setCreatedDate(baseRolePermission.getCreatedDate());
        return entity;
    }

    public BaseRolePermission toDomain(BaseRolePermissionJpaEntity entity) {
        return new BaseRolePermission(
            entity.getId(),
            entity.getBaseRole().getId(),
            entity.getBasePermission().getId(),
            entity.getCreatedDate()
        );
    }
}
