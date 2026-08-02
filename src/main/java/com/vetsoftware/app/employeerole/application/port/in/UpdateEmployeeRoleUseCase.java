package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.employeerole.application.command.UpdateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateEmployeeRoleUseCase {
  @PreAuthorize("hasRole('SYSTEM')")
  EmployeeRoleDto execute(UpdateEmployeeRoleCommand command);
}
