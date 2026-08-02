package com.vetsoftware.app.rolepermission.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteRolePermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('rolePermissions.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
