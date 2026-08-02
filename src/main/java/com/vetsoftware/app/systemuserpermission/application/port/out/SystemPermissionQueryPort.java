package com.vetsoftware.app.systemuserpermission.application.port.out;

import com.vetsoftware.app.systemuserpermission.domain.SystemPermissionRef;
import java.util.Optional;

public interface SystemPermissionQueryPort {
  Optional<SystemPermissionRef> findById(Long systemPermissionId);
}
