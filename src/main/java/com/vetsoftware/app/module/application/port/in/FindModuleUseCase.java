package com.vetsoftware.app.module.application.port.in;

import com.vetsoftware.app.module.application.dto.ModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindModuleUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  ModuleDto findById(Long id);
}
