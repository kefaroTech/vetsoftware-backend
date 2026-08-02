package com.vetsoftware.app.spa.application.port.in;

import com.vetsoftware.app.spa.application.command.CreateSpaCommand;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateSpaUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('spa.create')")
  SpaDto execute(CreateSpaCommand command);
}
