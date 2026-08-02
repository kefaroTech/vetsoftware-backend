package com.vetsoftware.app.deworming.application.port.in;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateDewormingUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('deworming.update')")
  DewormingDto execute(Long id);
}
