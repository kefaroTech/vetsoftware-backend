package com.vetsoftware.app.basepermission.application.port.in;

import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindBasePermissionUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  BasePermissionDto findById(Long id);
}
