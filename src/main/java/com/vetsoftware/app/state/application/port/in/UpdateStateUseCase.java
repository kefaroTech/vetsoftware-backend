package com.vetsoftware.app.state.application.port.in;

import com.vetsoftware.app.state.application.command.UpdateStateCommand;
import com.vetsoftware.app.state.application.dto.StateDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateStateUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  StateDto execute(UpdateStateCommand command);
}
