package com.vetsoftware.app.module.application.usecase;

import com.vetsoftware.app.module.application.command.CreateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;
import com.vetsoftware.app.module.application.port.in.CreateModuleUseCase;
import com.vetsoftware.app.module.application.port.out.ModuleRepository;
import com.vetsoftware.app.module.domain.Module;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "module.create")
@Service
public class CreateModuleService implements CreateModuleUseCase {
  private final ModuleRepository repository;

  public CreateModuleService(ModuleRepository repository) {
    this.repository = repository;
  }

  @Override
  public ModuleDto execute(CreateModuleCommand command) {
    return ModuleDto.from(repository.save(Module.create(command.name(), command.code())));
  }
}
