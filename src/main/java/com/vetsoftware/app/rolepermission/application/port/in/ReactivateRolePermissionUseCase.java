package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateRolePermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('rolePermissions.update') and @authz.isMyCompany(#companyId))")
    RolePermissionDto execute(Long id, Long companyId);
}
