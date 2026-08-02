package com.vetsoftware.app.role.infrastructure.persistence;

import com.vetsoftware.app.role.application.port.out.RolePermissionChildrenCascadePort;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaRolePermissionChildrenCascadePort implements RolePermissionChildrenCascadePort {
  private final RolePermissionJpaRepository jpaRepository;

  public JpaRolePermissionChildrenCascadePort(RolePermissionJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public int deactivateAllByRoleId(Long roleId) {
    return jpaRepository.disableAllByRoleId(roleId);
  }
}
