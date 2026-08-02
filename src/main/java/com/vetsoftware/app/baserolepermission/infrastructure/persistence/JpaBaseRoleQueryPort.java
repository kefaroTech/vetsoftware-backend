package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRoleQueryPort;
import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("baserolepermissionJpaBaseRoleQueryPort")
public class JpaBaseRoleQueryPort implements BaseRoleQueryPort {
  private final BaseRoleJpaRepository baseRoleJpaRepository;

  public JpaBaseRoleQueryPort(BaseRoleJpaRepository baseRoleJpaRepository) {
    this.baseRoleJpaRepository = baseRoleJpaRepository;
  }

  @Override
  public Optional<BaseRoleRef> findById(Long baseRoleId) {
    return baseRoleJpaRepository
        .findById(baseRoleId)
        .map(e -> new BaseRoleRef(e.getId(), e.getName(), e.getCode()));
  }
}
