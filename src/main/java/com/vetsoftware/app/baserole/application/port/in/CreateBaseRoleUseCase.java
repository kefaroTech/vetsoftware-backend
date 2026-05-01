package com.vetsoftware.app.baserole.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserole.application.command.CreateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;

public interface CreateBaseRoleUseCase {
    @RequiresPermission("admin.all")
    BaseRoleDto execute(CreateBaseRoleCommand command, AuthContext auth);
}
