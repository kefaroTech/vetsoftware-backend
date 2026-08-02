package com.vetsoftware.app.submodule.application.port.out;

import com.vetsoftware.app.submodule.domain.ModuleRef;
import java.util.Optional;

public interface ModuleQueryPort {
  Optional<ModuleRef> findById(Long moduleId);
}
