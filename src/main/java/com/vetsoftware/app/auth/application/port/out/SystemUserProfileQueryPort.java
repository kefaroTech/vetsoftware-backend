package com.vetsoftware.app.auth.application.port.out;

import java.util.Optional;

public interface SystemUserProfileQueryPort {
  Optional<SystemUserProfile> findById(Long systemUserId);

  record SystemUserProfile(Long id, String code) {}
}
