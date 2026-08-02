package com.vetsoftware.app.permission.application.port.out;

import com.vetsoftware.app.permission.domain.Permission;
import java.util.List;
import java.util.Optional;

public interface PermissionRepository {
  Permission save(Permission permission);

  Optional<Permission> findById(Long id);

  List<Permission> findAll();

  List<Permission> findAllByCompanyId(Long companyId);

  void delete(Long id);

  int reactivate(Long id);
}
