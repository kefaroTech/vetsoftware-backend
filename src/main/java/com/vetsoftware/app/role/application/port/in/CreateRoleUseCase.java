package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.role.application.command.CreateRoleCommand;
import com.vetsoftware.app.role.application.dto.RoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateRoleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    RoleDto execute(CreateRoleCommand command);
}
