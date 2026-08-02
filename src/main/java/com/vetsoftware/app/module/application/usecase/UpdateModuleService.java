package com.vetsoftware.app.module.application.usecase;

import com.vetsoftware.app.module.application.command.UpdateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;
import com.vetsoftware.app.module.application.port.in.UpdateModuleUseCase;
import com.vetsoftware.app.module.application.port.out.ModuleRepository;
import com.vetsoftware.app.module.domain.Module;
import com.vetsoftware.app.module.domain.ModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "module.update")
@Service
public class UpdateModuleService implements UpdateModuleUseCase {
  private final ModuleRepository repository;

  public UpdateModuleService(ModuleRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public ModuleDto execute(UpdateModuleCommand command) {
    Module module =
        repository
            .findById(command.id())
            .orElseThrow(() -> new ModuleNotFoundException(command.id()));
    module.update(command.name(), command.code());
    return ModuleDto.from(repository.save(module));
  }
}
