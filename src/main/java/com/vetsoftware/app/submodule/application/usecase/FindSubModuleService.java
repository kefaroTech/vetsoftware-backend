package com.vetsoftware.app.submodule.application.usecase;

import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.in.FindSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "submodule.find")
@Service
public class FindSubModuleService implements FindSubModuleUseCase {
  private final SubModuleRepository repository;

  public FindSubModuleService(SubModuleRepository repository) {
    this.repository = repository;
  }

  @Override
  public SubModuleDto findById(Long id) {
    return SubModuleDto.from(
        repository.findById(id).orElseThrow(() -> new SubModuleNotFoundException(id)));
  }
}
