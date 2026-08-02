package com.vetsoftware.app.systemuserpermission.application.port.in;

import com.vetsoftware.app.systemuserpermission.application.command.UpdateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateSystemUserPermissionUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  SystemUserPermissionDto execute(UpdateSystemUserPermissionCommand command);
}
