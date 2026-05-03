package com.vetsoftware.app.systempermission.application.port.in;

import com.vetsoftware.app.systempermission.application.command.CreateSystemPermissionCommand;
import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSystemPermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    SystemPermissionDto execute(CreateSystemPermissionCommand command);
}
