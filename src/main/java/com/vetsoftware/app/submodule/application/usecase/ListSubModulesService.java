package com.vetsoftware.app.submodule.application.usecase;

import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.in.ListSubModulesUseCase;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "submodule.list")
@Service
public class ListSubModulesService implements ListSubModulesUseCase {
  private final SubModuleRepository repository;

  public ListSubModulesService(SubModuleRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SubModuleDto> listAll() {
    return repository.findAll().stream().map(SubModuleDto::from).toList();
  }
}
