package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.role.application.dto.RoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateRoleUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('rolePermissions.update') and @authz.isMyCompany(#companyId))")
    RoleDto execute(Long id, Long companyId);
}
