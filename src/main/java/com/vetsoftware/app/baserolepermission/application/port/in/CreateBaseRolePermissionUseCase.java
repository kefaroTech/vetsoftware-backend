package com.vetsoftware.app.baserolepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserolepermission.application.command.CreateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;

public interface CreateBaseRolePermissionUseCase {
    @RequiresPermission("admin.all")
    BaseRolePermissionDto execute(CreateBaseRolePermissionCommand command, AuthContext auth);
}
