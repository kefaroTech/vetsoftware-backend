package com.vetsoftware.app.baserolepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserolepermission.application.command.UpdateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;

public interface UpdateBaseRolePermissionUseCase {
    @RequiresPermission("admin.all")
    BaseRolePermissionDto execute(UpdateBaseRolePermissionCommand command, AuthContext auth);
}
