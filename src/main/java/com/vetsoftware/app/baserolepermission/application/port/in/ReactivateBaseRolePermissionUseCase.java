package com.vetsoftware.app.baserolepermission.application.port.in;

import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateBaseRolePermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('base_role_permission.update') or hasRole('SYSTEM')")
    BaseRolePermissionDto execute(Long id);
}
