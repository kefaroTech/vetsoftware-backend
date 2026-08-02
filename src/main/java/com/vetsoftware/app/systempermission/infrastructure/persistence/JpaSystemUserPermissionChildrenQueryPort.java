package com.vetsoftware.app.systempermission.infrastructure.persistence;

import com.vetsoftware.app.systempermission.application.port.out.SystemUserPermissionChildrenQueryPort;
import com.vetsoftware.app.systemuserpermission.infrastructure.persistence.SystemUserPermissionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaSystemUserPermissionChildrenQueryPort
    implements SystemUserPermissionChildrenQueryPort {
  private final SystemUserPermissionJpaRepository jpaRepository;

  public JpaSystemUserPermissionChildrenQueryPort(SystemUserPermissionJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveBySystemPermissionId(Long parentId) {
    return jpaRepository.existsBySystemPermission_Id(parentId);
  }
}
