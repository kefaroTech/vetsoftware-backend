package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.permission.application.command.CreatePermissionCommand;
import com.vetsoftware.app.permission.application.dto.PermissionDto;

public interface CreatePermissionUseCase {
    @RequiresPermission("admin.all")
    PermissionDto execute(CreatePermissionCommand command, AuthContext auth);
}
