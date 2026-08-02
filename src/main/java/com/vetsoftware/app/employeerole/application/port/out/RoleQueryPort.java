package com.vetsoftware.app.employeerole.application.port.out;

import com.vetsoftware.app.employeerole.domain.RoleRef;
import java.util.Optional;

public interface RoleQueryPort {
  Optional<RoleRef> findById(Long roleId);
}
