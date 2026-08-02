package com.vetsoftware.app.basepermission.application.port.in;

import com.vetsoftware.app.basepermission.application.command.UpdateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateBasePermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    BasePermissionDto execute(UpdateBasePermissionCommand command);
}
