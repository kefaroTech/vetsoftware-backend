package com.vetsoftware.app.systempermission.application.port.out;

import com.vetsoftware.app.systempermission.domain.SystemPermission;
import java.util.List;
import java.util.Optional;

public interface SystemPermissionRepository {
    SystemPermission save(SystemPermission systemPermission);
    Optional<SystemPermission> findById(Long id);
    List<SystemPermission> findAll();
    void delete(Long id);
    int reactivate(Long id);
}
