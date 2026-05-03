package com.vetsoftware.app.basepermission.application.port.in;

import com.vetsoftware.app.basepermission.application.command.CreateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateBasePermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    BasePermissionDto execute(CreateBasePermissionCommand command);
}
