package com.vetsoftware.app.systemuserpermission.infrastructure.persistence;

import com.vetsoftware.app.systempermission.infrastructure.persistence.SystemPermissionJpaRepository;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemPermissionQueryPort;
import com.vetsoftware.app.systemuserpermission.domain.SystemPermissionRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("systemuserpermissionJpaSystemPermissionQueryPort")
public class JpaSystemPermissionQueryPort implements SystemPermissionQueryPort {
  private final SystemPermissionJpaRepository systemPermissionJpaRepository;

  public JpaSystemPermissionQueryPort(SystemPermissionJpaRepository systemPermissionJpaRepository) {
    this.systemPermissionJpaRepository = systemPermissionJpaRepository;
  }

  @Override
  public Optional<SystemPermissionRef> findById(Long systemPermissionId) {
    return systemPermissionJpaRepository
        .findById(systemPermissionId)
        .map(e -> new SystemPermissionRef(e.getId(), e.getName(), e.getCode()));
  }
}
