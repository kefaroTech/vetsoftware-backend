package com.vetsoftware.app.rolepermission.application.port.out;

import com.vetsoftware.app.rolepermission.domain.RolePermission;
import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository {
    RolePermission save(RolePermission rolePermission);
    Optional<RolePermission> findById(Long id);
    List<RolePermission> findAll();
    void delete(Long id);
}
