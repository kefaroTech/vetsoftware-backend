package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateEmployeeRoleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('employeerole.update') or hasRole('SYSTEM')")
    EmployeeRoleDto execute(Long id);
}
