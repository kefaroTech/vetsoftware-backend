package com.vetsoftware.app.state.application.port.in;

import com.vetsoftware.app.state.application.command.CreateStateCommand;
import com.vetsoftware.app.state.application.dto.StateDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateStateUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  StateDto execute(CreateStateCommand command);
}
