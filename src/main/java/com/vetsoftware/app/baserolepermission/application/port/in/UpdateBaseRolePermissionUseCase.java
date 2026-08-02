package com.vetsoftware.app.baserolepermission.application.port.in;

import com.vetsoftware.app.baserolepermission.application.command.UpdateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateBaseRolePermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    BaseRolePermissionDto execute(UpdateBaseRolePermissionCommand command);
}
