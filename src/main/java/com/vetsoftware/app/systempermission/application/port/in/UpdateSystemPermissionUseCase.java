package com.vetsoftware.app.systempermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systempermission.application.command.UpdateSystemPermissionCommand;
import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;

public interface UpdateSystemPermissionUseCase {
    @RequiresPermission("admin.all")
    SystemPermissionDto execute(UpdateSystemPermissionCommand command, AuthContext auth);
}
