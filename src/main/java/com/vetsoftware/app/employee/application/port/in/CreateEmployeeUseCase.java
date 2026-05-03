package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateEmployeeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('employee.create') or hasRole('SYSTEM')")
    EmployeeDto execute(CreateEmployeeCommand command);
}
