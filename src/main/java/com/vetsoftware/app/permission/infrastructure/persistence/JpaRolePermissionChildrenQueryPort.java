package com.vetsoftware.app.permission.infrastructure.persistence;

import com.vetsoftware.app.permission.application.port.out.RolePermissionChildrenQueryPort;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaRolePermissionChildrenQueryPort implements RolePermissionChildrenQueryPort {
  private final RolePermissionJpaRepository jpaRepository;

  public JpaRolePermissionChildrenQueryPort(RolePermissionJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByPermissionId(Long parentId) {
    return jpaRepository.existsByPermission_Id(parentId);
  }
}
