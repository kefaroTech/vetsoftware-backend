package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.module.application.command.CreateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;

public interface CreateModuleUseCase {
    ModuleDto execute(CreateModuleCommand command);
}
