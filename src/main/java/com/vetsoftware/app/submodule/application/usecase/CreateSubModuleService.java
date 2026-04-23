package com.vetsoftware.app.submodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.submodule.application.command.CreateSubModuleCommand;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.in.CreateSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.out.ModuleValidationPort;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.SubModule;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "submodule.create")
@Service
public class CreateSubModuleService implements CreateSubModuleUseCase {
    private final SubModuleRepository repository;
    private final ModuleValidationPort moduleValidationPort;

    public CreateSubModuleService(SubModuleRepository repository,
                                  ModuleValidationPort moduleValidationPort) {
        this.repository = repository;
        this.moduleValidationPort = moduleValidationPort;
    }

    @Override
    public SubModuleDto execute(CreateSubModuleCommand command, AuthContext auth) {
        moduleValidationPort.validateExists(command.moduleId());
        SubModule subModule = SubModule.create(command.name(), command.code(), command.moduleId());
        return SubModuleDto.from(repository.save(subModule));
    }
}
