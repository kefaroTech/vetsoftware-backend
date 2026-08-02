package com.vetsoftware.app.baserolepermission.application.port.out;

import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import java.util.List;
import java.util.Optional;

public interface BaseRolePermissionRepository {
  BaseRolePermission save(BaseRolePermission baseRolePermission);

  Optional<BaseRolePermission> findById(Long id);

  List<BaseRolePermission> findAll();

  void delete(Long id);

  int reactivate(Long id);

  Optional<Long> findDisabledIdByBaseRoleAndBasePermission(Long baseRoleId, Long basePermissionId);
}
