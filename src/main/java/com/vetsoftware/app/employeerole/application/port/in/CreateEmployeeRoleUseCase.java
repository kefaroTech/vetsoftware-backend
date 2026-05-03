package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateEmployeeRoleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    EmployeeRoleDto execute(CreateEmployeeRoleCommand command);
}
