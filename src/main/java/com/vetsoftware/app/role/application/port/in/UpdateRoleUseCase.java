package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.role.application.command.UpdateRoleCommand;
import com.vetsoftware.app.role.application.dto.RoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateRoleUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('rolePermissions.update') and @authz.isMyCompany(#command.companyId))")
    RoleDto execute(UpdateRoleCommand command);
}
