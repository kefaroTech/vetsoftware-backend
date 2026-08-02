package com.vetsoftware.app.membershipsubmodule.application.port.out;

import com.vetsoftware.app.membershipsubmodule.domain.SubModuleRef;
import java.util.Optional;

public interface SubModuleQueryPort {
  Optional<SubModuleRef> findById(Long subModuleId);
}
