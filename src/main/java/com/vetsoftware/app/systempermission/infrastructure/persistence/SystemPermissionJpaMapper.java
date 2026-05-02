package com.vetsoftware.app.systempermission.infrastructure.persistence;

import com.vetsoftware.app.systempermission.domain.SystemPermission;
import org.springframework.stereotype.Component;

@Component
public class SystemPermissionJpaMapper {

    public SystemPermissionJpaEntity toJpa(SystemPermission systemPermission) {
        SystemPermissionJpaEntity entity = new SystemPermissionJpaEntity();
        entity.setId(systemPermission.getId());
        entity.setName(systemPermission.getName());
        entity.setCode(systemPermission.getCode());
        entity.setCreatedDate(systemPermission.getCreatedDate());
        return entity;
    }

    public SystemPermission toDomain(SystemPermissionJpaEntity entity) {
        return new SystemPermission(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            entity.getCreatedDate()
        );
    }
}
