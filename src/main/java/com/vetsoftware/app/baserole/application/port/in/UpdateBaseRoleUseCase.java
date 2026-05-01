package com.vetsoftware.app.baserole.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserole.application.command.UpdateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;

public interface UpdateBaseRoleUseCase {
    @RequiresPermission("admin.all")
    BaseRoleDto execute(UpdateBaseRoleCommand command, AuthContext auth);
}
