package com.vetsoftware.app.submodule.application.usecase;

import com.vetsoftware.app.submodule.application.command.CreateSubModuleCommand;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.in.CreateSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.out.ModuleQueryPort;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.ModuleRef;
import com.vetsoftware.app.submodule.domain.SubModule;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "submodule.create")
@Service
public class CreateSubModuleService implements CreateSubModuleUseCase {
    private final SubModuleRepository repository;
    private final ModuleQueryPort moduleQueryPort;

    public CreateSubModuleService(SubModuleRepository repository,
                                  ModuleQueryPort moduleQueryPort) {
        this.repository = repository;
        this.moduleQueryPort = moduleQueryPort;
    }

    @Override
    public SubModuleDto execute(CreateSubModuleCommand command) {
        ModuleRef module = moduleQueryPort.findById(command.moduleId())
            .orElseThrow(() -> new IllegalArgumentException("Module not found: " + command.moduleId()));
        SubModule subModule = SubModule.create(command.name(), command.code(), module);
        return SubModuleDto.from(repository.save(subModule));
    }
}
