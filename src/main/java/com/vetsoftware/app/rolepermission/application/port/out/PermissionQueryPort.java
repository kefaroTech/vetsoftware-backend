package com.vetsoftware.app.rolepermission.application.port.out;

import com.vetsoftware.app.rolepermission.domain.PermissionRef;
import java.util.Optional;

public interface PermissionQueryPort {
    Optional<PermissionRef> findById(Long permissionId);
    Optional<PermissionRef> findByIdAndCompanyId(Long permissionId, Long companyId);
}
