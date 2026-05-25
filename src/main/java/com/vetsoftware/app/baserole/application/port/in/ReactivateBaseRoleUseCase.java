package com.vetsoftware.app.baserole.application.port.in;

import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateBaseRoleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('base_role.update') or hasRole('SYSTEM')")
    BaseRoleDto execute(Long id);
}
