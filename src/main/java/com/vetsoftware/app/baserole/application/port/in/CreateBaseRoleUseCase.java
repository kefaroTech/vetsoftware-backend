package com.vetsoftware.app.baserole.application.port.in;

import com.vetsoftware.app.baserole.application.command.CreateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateBaseRoleUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  BaseRoleDto execute(CreateBaseRoleCommand command);
}
