package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.SystemUserProfileQueryPort;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSystemUserProfileQueryPort implements SystemUserProfileQueryPort {

  private final SystemUserJpaRepository systemUserJpaRepository;

  public JpaSystemUserProfileQueryPort(SystemUserJpaRepository systemUserJpaRepository) {
    this.systemUserJpaRepository = systemUserJpaRepository;
  }

  @Override
  public Optional<SystemUserProfile> findById(Long systemUserId) {
    return systemUserJpaRepository
        .findById(systemUserId)
        .map(u -> new SystemUserProfile(u.getId(), u.getCode()));
  }
}
