package com.vetsoftware.app.baserolepermission.application.port.in;

import com.vetsoftware.app.baserolepermission.application.command.CreateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateBaseRolePermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    BaseRolePermissionDto execute(CreateBaseRolePermissionCommand command);
}
