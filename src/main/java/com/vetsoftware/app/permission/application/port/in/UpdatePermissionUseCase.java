package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.permission.application.command.UpdatePermissionCommand;
import com.vetsoftware.app.permission.application.dto.PermissionDto;

public interface UpdatePermissionUseCase {
    @RequiresPermission("admin.all")
    PermissionDto execute(UpdatePermissionCommand command, AuthContext auth);
}
