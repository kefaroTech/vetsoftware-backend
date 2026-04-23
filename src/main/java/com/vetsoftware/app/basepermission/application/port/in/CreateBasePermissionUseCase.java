package com.vetsoftware.app.basepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.basepermission.application.command.CreateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;

public interface CreateBasePermissionUseCase {
    @RequiresPermission("admin.all")
    BasePermissionDto execute(CreateBasePermissionCommand command, AuthContext auth);
}
