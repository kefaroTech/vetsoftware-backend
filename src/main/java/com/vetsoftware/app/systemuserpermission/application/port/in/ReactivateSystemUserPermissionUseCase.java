package com.vetsoftware.app.systemuserpermission.application.port.in;

import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSystemUserPermissionUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('systemuserpermission.update')")
  SystemUserPermissionDto execute(Long id);
}
