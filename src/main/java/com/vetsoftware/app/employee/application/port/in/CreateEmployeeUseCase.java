package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateEmployeeUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('employee.create')")
  EmployeeDto execute(CreateEmployeeCommand command);
}
