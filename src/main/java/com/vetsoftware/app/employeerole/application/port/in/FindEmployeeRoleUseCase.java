package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindEmployeeRoleUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    EmployeeRoleDto findById(Long id);
}
