package com.vetsoftware.app.submodule.application.port.out;

import com.vetsoftware.app.submodule.domain.SubModule;
import java.util.List;
import java.util.Optional;

public interface SubModuleRepository {
  SubModule save(SubModule subModule);

  Optional<SubModule> findById(Long id);

  List<SubModule> findAll();

  void delete(Long id);

  int reactivate(Long id);
}
