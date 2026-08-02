package com.vetsoftware.app.role.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteRoleUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('rolePermissions.delete') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
