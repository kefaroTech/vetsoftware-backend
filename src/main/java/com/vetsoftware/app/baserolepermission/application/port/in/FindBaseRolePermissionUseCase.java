package com.vetsoftware.app.baserolepermission.application.port.in;

import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindBaseRolePermissionUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  BaseRolePermissionDto findById(Long id);
}
