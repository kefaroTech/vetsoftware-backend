package com.vetsoftware.app.baserole.application.port.in;

import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateBaseRoleUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('base_role.update')")
  BaseRoleDto execute(Long id);
}
