package com.vetsoftware.app.baserole.infrastructure.persistence;

import com.vetsoftware.app.baserole.application.port.out.BaseRolePermissionChildrenQueryPort;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaBaseRolePermissionChildrenQueryPort implements BaseRolePermissionChildrenQueryPort {
  private final BaseRolePermissionJpaRepository jpaRepository;

  public JpaBaseRolePermissionChildrenQueryPort(BaseRolePermissionJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByBaseRoleId(Long parentId) {
    return jpaRepository.existsByBaseRole_Id(parentId);
  }
}
