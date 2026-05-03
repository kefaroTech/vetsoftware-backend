package com.vetsoftware.app.baserole.application.port.in;

import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindBaseRoleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    BaseRoleDto findById(Long id);
}
