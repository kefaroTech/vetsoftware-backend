package com.vetsoftware.app.baserole.infrastructure.persistence;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaRepository;
import com.vetsoftware.app.baserole.application.port.out.BaseRolePermissionInitializationPort;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class JpaBaseRolePermissionInitializationPort
    implements BaseRolePermissionInitializationPort {
  private final BaseRoleJpaRepository baseRoleJpaRepository;
  private final BasePermissionJpaRepository basePermissionJpaRepository;
  private final BaseRolePermissionJpaRepository baseRolePermissionJpaRepository;

  public JpaBaseRolePermissionInitializationPort(
      BaseRoleJpaRepository baseRoleJpaRepository,
      BasePermissionJpaRepository basePermissionJpaRepository,
      BaseRolePermissionJpaRepository baseRolePermissionJpaRepository) {
    this.baseRoleJpaRepository = baseRoleJpaRepository;
    this.basePermissionJpaRepository = basePermissionJpaRepository;
    this.baseRolePermissionJpaRepository = baseRolePermissionJpaRepository;
  }

  @Override
  public void initializeForAllBasePermissions(Long baseRoleId) {
    var baseRole = baseRoleJpaRepository.getReferenceById(baseRoleId);
    var entities =
        basePermissionJpaRepository.findAll().stream()
            .filter(
                bp ->
                    !baseRolePermissionJpaRepository.existsByBaseRoleIdAndBasePermissionId(
                        baseRoleId, bp.getId()))
            .map(
                bp -> {
                  var entity = new BaseRolePermissionJpaEntity();
                  entity.setBaseRole(baseRole);
                  entity.setBasePermission(bp);
                  entity.setCreatedDate(LocalDateTime.now());
                  return entity;
                })
            .toList();
    baseRolePermissionJpaRepository.saveAll(entities);
  }
}
