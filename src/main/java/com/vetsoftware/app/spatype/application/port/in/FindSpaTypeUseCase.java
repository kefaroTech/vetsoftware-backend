package com.vetsoftware.app.spatype.application.port.in;

import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSpaTypeUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  SpaTypeDto findById(Long id);
}
