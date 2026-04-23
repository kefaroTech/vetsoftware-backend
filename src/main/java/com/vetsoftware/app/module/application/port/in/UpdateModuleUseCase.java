package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.module.application.command.UpdateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;

public interface UpdateModuleUseCase {
    @RequiresPermission("module.update")
    ModuleDto execute(UpdateModuleCommand command, AuthContext auth);
}
