package com.vetsoftware.app.systemuserpermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systemuserpermission.application.command.UpdateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;

public interface UpdateSystemUserPermissionUseCase {
    @RequiresPermission("admin.all")
    SystemUserPermissionDto execute(UpdateSystemUserPermissionCommand command, AuthContext auth);
}
