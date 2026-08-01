package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.role.application.dto.RoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindRoleUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    RoleDto findById(Long id);
}
