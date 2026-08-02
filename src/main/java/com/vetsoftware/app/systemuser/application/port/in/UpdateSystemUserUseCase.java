package com.vetsoftware.app.systemuser.application.port.in;

import com.vetsoftware.app.systemuser.application.command.UpdateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateSystemUserUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  SystemUserDto execute(UpdateSystemUserCommand command);
}
