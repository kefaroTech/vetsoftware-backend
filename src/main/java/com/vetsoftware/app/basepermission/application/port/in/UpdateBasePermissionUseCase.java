package com.vetsoftware.app.basepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.basepermission.application.command.UpdateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;

public interface UpdateBasePermissionUseCase {
    @RequiresPermission("admin.all")
    BasePermissionDto execute(UpdateBasePermissionCommand command, AuthContext auth);
}
