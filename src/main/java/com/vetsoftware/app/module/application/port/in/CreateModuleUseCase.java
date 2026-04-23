package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.module.application.command.CreateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;

public interface CreateModuleUseCase {
    @RequiresPermission("module.create")
    ModuleDto execute(CreateModuleCommand command, AuthContext auth);
}
