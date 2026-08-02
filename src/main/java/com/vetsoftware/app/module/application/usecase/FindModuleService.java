package com.vetsoftware.app.module.application.usecase;

import com.vetsoftware.app.module.application.dto.ModuleDto;
import com.vetsoftware.app.module.application.port.in.FindModuleUseCase;
import com.vetsoftware.app.module.application.port.out.ModuleRepository;
import com.vetsoftware.app.module.domain.ModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "module.find")
@Service
public class FindModuleService implements FindModuleUseCase {
  private final ModuleRepository repository;

  public FindModuleService(ModuleRepository repository) {
    this.repository = repository;
  }

  @Override
  public ModuleDto findById(Long id) {
    return repository
        .findById(id)
        .map(ModuleDto::from)
        .orElseThrow(() -> new ModuleNotFoundException(id));
  }
}
