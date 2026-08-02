package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.rolepermission.domain.PermissionRef;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import com.vetsoftware.app.rolepermission.domain.RoleRef;
import org.springframework.stereotype.Component;

@Component
public class RolePermissionJpaMapper {

  public RolePermissionJpaEntity toJpa(
      RolePermission rolePermission, RoleJpaEntity role, PermissionJpaEntity permission) {
    RolePermissionJpaEntity entity = new RolePermissionJpaEntity();
    entity.setId(rolePermission.getId());
    entity.setRole(role);
    entity.setPermission(permission);
    entity.setCreatedDate(rolePermission.getCreatedDate());
    entity.setEnabled(rolePermission.isEnabled());
    return entity;
  }

  public RolePermission toDomain(RolePermissionJpaEntity entity) {
    RoleJpaEntity r = entity.getRole();
    PermissionJpaEntity p = entity.getPermission();
    return toDomain(
        entity,
        new RoleRef(r.getId(), r.getName(), r.getCode()),
        new PermissionRef(p.getId(), p.getName(), p.getCode()));
  }

  public RolePermission toDomain(
      RolePermissionJpaEntity entity, RoleRef roleRef, PermissionRef permissionRef) {
    return new RolePermission(
        entity.getId(), roleRef, permissionRef, entity.getCreatedDate(), entity.isEnabled());
  }
}
