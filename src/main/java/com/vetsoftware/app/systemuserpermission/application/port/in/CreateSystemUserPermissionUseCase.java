package com.vetsoftware.app.systemuserpermission.application.port.in;

import com.vetsoftware.app.systemuserpermission.application.command.CreateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSystemUserPermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    SystemUserPermissionDto execute(CreateSystemUserPermissionCommand command);
}
