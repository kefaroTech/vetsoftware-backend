package com.vetsoftware.app.submodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.submodule.application.command.UpdateSubModuleCommand;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.in.UpdateSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.out.ModuleQueryPort;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.ModuleRef;
import com.vetsoftware.app.submodule.domain.SubModule;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "submodule.update")
@Service
public class UpdateSubModuleService implements UpdateSubModuleUseCase {
    private final SubModuleRepository repository;
    private final ModuleQueryPort moduleQueryPort;

    public UpdateSubModuleService(SubModuleRepository repository,
                                  ModuleQueryPort moduleQueryPort) {
        this.repository = repository;
        this.moduleQueryPort = moduleQueryPort;
    }

    @Override
    @Transactional
    public SubModuleDto execute(UpdateSubModuleCommand command, AuthContext auth) {
        SubModule subModule = repository.findById(command.id())
            .orElseThrow(() -> new SubModuleNotFoundException(command.id()));
        ModuleRef module = moduleQueryPort.findById(command.moduleId())
            .orElseThrow(() -> new IllegalArgumentException("Module not found: " + command.moduleId()));
        subModule.update(command.name(), command.code(), module);
        return SubModuleDto.from(repository.save(subModule));
    }
}
