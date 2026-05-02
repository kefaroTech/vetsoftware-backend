package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.rolepermission.application.command.CreateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;

public interface CreateRolePermissionUseCase {
    @RequiresPermission("admin.all")
    RolePermissionDto execute(CreateRolePermissionCommand command, AuthContext auth);
}
