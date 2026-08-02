package com.vetsoftware.app.systempermission.application.port.in;

import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSystemPermissionUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  SystemPermissionDto findById(Long id);
}
