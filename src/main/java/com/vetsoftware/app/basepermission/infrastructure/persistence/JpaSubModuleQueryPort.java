package com.vetsoftware.app.basepermission.infrastructure.persistence;

import com.vetsoftware.app.basepermission.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.basepermission.domain.SubModuleRef;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("basepermissionJpaSubModuleQueryPort")
public class JpaSubModuleQueryPort implements SubModuleQueryPort {
  private final SubModuleJpaRepository subModuleJpaRepository;

  public JpaSubModuleQueryPort(SubModuleJpaRepository subModuleJpaRepository) {
    this.subModuleJpaRepository = subModuleJpaRepository;
  }

  @Override
  public Optional<SubModuleRef> findById(Long subModuleId) {
    return subModuleJpaRepository
        .findById(subModuleId)
        .map(e -> new SubModuleRef(e.getId(), e.getName(), e.getCode()));
  }
}
