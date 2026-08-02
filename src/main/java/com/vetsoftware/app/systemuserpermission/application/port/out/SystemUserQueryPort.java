package com.vetsoftware.app.systemuserpermission.application.port.out;

import com.vetsoftware.app.systemuserpermission.domain.SystemUserRef;
import java.util.Optional;

public interface SystemUserQueryPort {
  Optional<SystemUserRef> findById(Long systemUserId);
}
