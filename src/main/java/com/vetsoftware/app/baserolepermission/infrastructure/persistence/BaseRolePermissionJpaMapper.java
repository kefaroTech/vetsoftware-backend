package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaEntity;
import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;
import org.springframework.stereotype.Component;

@Component
public class BaseRolePermissionJpaMapper {

  public BaseRolePermissionJpaEntity toJpa(
      BaseRolePermission baseRolePermission,
      BaseRoleJpaEntity baseRole,
      BasePermissionJpaEntity basePermission) {
    BaseRolePermissionJpaEntity entity = new BaseRolePermissionJpaEntity();
    entity.setId(baseRolePermission.getId());
    entity.setBaseRole(baseRole);
    entity.setBasePermission(basePermission);
    entity.setCreatedDate(baseRolePermission.getCreatedDate());
    entity.setEnabled(baseRolePermission.isEnabled());
    return entity;
  }

  public BaseRolePermission toDomain(BaseRolePermissionJpaEntity entity) {
    BaseRoleJpaEntity br = entity.getBaseRole();
    BasePermissionJpaEntity bp = entity.getBasePermission();
    return toDomain(
        entity,
        new BaseRoleRef(br.getId(), br.getName(), br.getCode()),
        new BasePermissionRef(bp.getId(), bp.getName(), bp.getCode()));
  }

  public BaseRolePermission toDomain(
      BaseRolePermissionJpaEntity entity,
      BaseRoleRef baseRoleRef,
      BasePermissionRef basePermissionRef) {
    return new BaseRolePermission(
        entity.getId(),
        baseRoleRef,
        basePermissionRef,
        entity.getCreatedDate(),
        entity.isEnabled());
  }
}
