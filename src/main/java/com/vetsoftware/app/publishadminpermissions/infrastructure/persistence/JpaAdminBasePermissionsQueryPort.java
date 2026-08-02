package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermission;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermissionsQueryPort;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaAdminBasePermissionsQueryPort implements AdminBasePermissionsQueryPort {

  private final BaseRolePermissionJpaRepository baseRolePermissionJpaRepository;

  public JpaAdminBasePermissionsQueryPort(
      BaseRolePermissionJpaRepository baseRolePermissionJpaRepository) {
    this.baseRolePermissionJpaRepository = baseRolePermissionJpaRepository;
  }

  @Override
  public List<AdminBasePermission> findByAdminBaseRoleId(Long adminBaseRoleId) {
    return baseRolePermissionJpaRepository.findByBaseRoleId(adminBaseRoleId).stream()
        .map(
            brp -> {
              BasePermissionJpaEntity bp = brp.getBasePermission();
              return new AdminBasePermission(
                  bp.getId(), bp.getCode(), bp.getName(), bp.getSubModule().getId());
            })
        .toList();
  }
}
