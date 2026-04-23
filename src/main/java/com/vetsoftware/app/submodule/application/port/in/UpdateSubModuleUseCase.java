package com.vetsoftware.app.submodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.submodule.application.command.UpdateSubModuleCommand;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;

public interface UpdateSubModuleUseCase {
    @RequiresPermission("admin.all")
    SubModuleDto execute(UpdateSubModuleCommand command, AuthContext auth);
}
