package com.vetsoftware.app.systemuser.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systemuser.application.command.UpdateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;

public interface UpdateSystemUserUseCase {
    @RequiresPermission("admin.all")
    SystemUserDto execute(UpdateSystemUserCommand command, AuthContext auth);
}
