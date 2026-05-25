package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.role.application.dto.RoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateRoleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('role.update') or hasRole('SYSTEM')")
    RoleDto execute(Long id);
}
