package com.vetsoftware.app.auth.application.port.in;

import com.vetsoftware.app.auth.application.dto.MeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetCurrentUserUseCase {
  @PreAuthorize("isAuthenticated()")
  MeDto execute();
}
