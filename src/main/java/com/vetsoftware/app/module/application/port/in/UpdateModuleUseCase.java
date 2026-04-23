package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.module.application.command.UpdateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;

public interface UpdateModuleUseCase {
    ModuleDto execute(UpdateModuleCommand command);
}
