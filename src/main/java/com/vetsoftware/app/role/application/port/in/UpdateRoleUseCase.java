package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.role.application.command.UpdateRoleCommand;
import com.vetsoftware.app.role.application.dto.RoleDto;

public interface UpdateRoleUseCase {
    @RequiresPermission("admin.all")
    RoleDto execute(UpdateRoleCommand command, AuthContext auth);
}
