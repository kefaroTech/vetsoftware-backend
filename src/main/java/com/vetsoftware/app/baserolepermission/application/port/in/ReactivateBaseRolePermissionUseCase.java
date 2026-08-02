package com.vetsoftware.app.baserolepermission.application.port.in;

import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateBaseRolePermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('base_role_permission.update')")
    BaseRolePermissionDto execute(Long id);
}
