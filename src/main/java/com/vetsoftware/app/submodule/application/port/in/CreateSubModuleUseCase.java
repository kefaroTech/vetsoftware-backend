package com.vetsoftware.app.submodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.submodule.application.command.CreateSubModuleCommand;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;

public interface CreateSubModuleUseCase {
    @RequiresPermission("admin.all")
    SubModuleDto execute(CreateSubModuleCommand command, AuthContext auth);
}
