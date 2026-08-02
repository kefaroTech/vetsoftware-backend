package com.vetsoftware.app.submodule.application.port.in;

import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSubModuleUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('submodule.update')")
  SubModuleDto execute(Long id);
}
