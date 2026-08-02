package com.vetsoftware.app.basepermission.application.port.out;

import com.vetsoftware.app.basepermission.domain.BasePermission;
import java.util.List;
import java.util.Optional;

public interface BasePermissionRepository {
  BasePermission save(BasePermission basePermission);

  Optional<BasePermission> findById(Long id);

  List<BasePermission> findAll();

  void delete(Long id);

  int reactivate(Long id);
}
