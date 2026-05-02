package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.role.application.command.CreateRoleCommand;
import com.vetsoftware.app.role.application.dto.RoleDto;

public interface CreateRoleUseCase {
    @RequiresPermission("admin.all")
    RoleDto execute(CreateRoleCommand command, AuthContext auth);
}
