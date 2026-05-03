package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.permission.application.command.UpdatePermissionCommand;
import com.vetsoftware.app.permission.application.dto.PermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdatePermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    PermissionDto execute(UpdatePermissionCommand command);
}
