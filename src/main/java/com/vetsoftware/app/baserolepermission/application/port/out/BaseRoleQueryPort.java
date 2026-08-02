package com.vetsoftware.app.baserolepermission.application.port.out;

import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;
import java.util.Optional;

public interface BaseRoleQueryPort {
  Optional<BaseRoleRef> findById(Long baseRoleId);
}
