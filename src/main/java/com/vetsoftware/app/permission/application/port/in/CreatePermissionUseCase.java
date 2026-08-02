package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.permission.application.command.CreatePermissionCommand;
import com.vetsoftware.app.permission.application.dto.PermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreatePermissionUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  PermissionDto execute(CreatePermissionCommand command);
}
