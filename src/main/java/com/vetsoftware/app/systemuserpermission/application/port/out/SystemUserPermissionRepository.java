package com.vetsoftware.app.systemuserpermission.application.port.out;

import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import java.util.List;
import java.util.Optional;

public interface SystemUserPermissionRepository {
    SystemUserPermission save(SystemUserPermission systemUserPermission);
    Optional<SystemUserPermission> findById(Long id);
    List<SystemUserPermission> findAll();
    void delete(Long id);
    int reactivate(Long id);
}
