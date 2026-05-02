package com.vetsoftware.app.systemuser.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systemuser.application.command.CreateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;

public interface CreateSystemUserUseCase {
    @RequiresPermission("admin.all")
    SystemUserDto execute(CreateSystemUserCommand command, AuthContext auth);
}
