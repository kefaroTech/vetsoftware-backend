package com.vetsoftware.app.systemuserpermission.application.port.in;

import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSystemUserPermissionUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  SystemUserPermissionDto findById(Long id);
}
