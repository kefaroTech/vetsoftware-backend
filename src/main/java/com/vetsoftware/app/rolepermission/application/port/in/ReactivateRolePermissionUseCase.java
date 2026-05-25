package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateRolePermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('role_permission.update') or hasRole('SYSTEM')")
    RolePermissionDto execute(Long id);
}
