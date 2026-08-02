package com.vetsoftware.app.submodule.application.usecase;

import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.in.ReactivateSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "submodule.reactivate")
@Service
public class ReactivateSubModuleService implements ReactivateSubModuleUseCase {
  private final SubModuleRepository repository;

  public ReactivateSubModuleService(SubModuleRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public SubModuleDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new SubModuleNotFoundException(id);
    return SubModuleDto.from(
        repository.findById(id).orElseThrow(() -> new SubModuleNotFoundException(id)));
  }
}
