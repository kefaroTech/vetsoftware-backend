package com.vetsoftware.app.systemuserpermission.infrastructure.persistence;

import com.vetsoftware.app.systempermission.infrastructure.persistence.SystemPermissionJpaEntity;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaEntity;
import com.vetsoftware.app.systemuserpermission.domain.SystemPermissionRef;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserRef;
import org.springframework.stereotype.Component;

@Component
public class SystemUserPermissionJpaMapper {

    public SystemUserPermissionJpaEntity toJpa(SystemUserPermission systemUserPermission,
            SystemUserJpaEntity systemUser, SystemPermissionJpaEntity systemPermission) {
        SystemUserPermissionJpaEntity entity = new SystemUserPermissionJpaEntity();
        entity.setId(systemUserPermission.getId());
        entity.setSystemUser(systemUser);
        entity.setSystemPermission(systemPermission);
        entity.setCreatedDate(systemUserPermission.getCreatedDate());
        entity.setEnabled(systemUserPermission.isEnabled());
        return entity;
    }

    public SystemUserPermission toDomain(SystemUserPermissionJpaEntity entity) {
        SystemUserJpaEntity u = entity.getSystemUser();
        SystemPermissionJpaEntity p = entity.getSystemPermission();
        return toDomain(entity, new SystemUserRef(u.getId(), u.getCode()),
                new SystemPermissionRef(p.getId(), p.getName(), p.getCode()));
    }

    public SystemUserPermission toDomain(SystemUserPermissionJpaEntity entity,
            SystemUserRef systemUserRef, SystemPermissionRef systemPermissionRef) {
        return new SystemUserPermission(entity.getId(), systemUserRef, systemPermissionRef,
                entity.getCreatedDate(), entity.isEnabled());
    }
}
