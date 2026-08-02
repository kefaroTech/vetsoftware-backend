package com.vetsoftware.app.systempermission.application.port.in;

import com.vetsoftware.app.systempermission.application.command.UpdateSystemPermissionCommand;
import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateSystemPermissionUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  SystemPermissionDto execute(UpdateSystemPermissionCommand command);
}
