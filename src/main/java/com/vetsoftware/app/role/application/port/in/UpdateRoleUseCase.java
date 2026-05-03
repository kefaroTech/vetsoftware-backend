package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.role.application.command.UpdateRoleCommand;
import com.vetsoftware.app.role.application.dto.RoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateRoleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    RoleDto execute(UpdateRoleCommand command);
}
