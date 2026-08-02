package com.vetsoftware.app.employee.infrastructure.orchestration;

import com.vetsoftware.app.employee.application.port.out.EmployeeRoleAssigner;
import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import org.springframework.stereotype.Component;

/**
 * Adapter de orquestación: asigna el rol vía la feature employeerole y devuelve el nombre del rol.
 */
@Component
public class EmployeeRoleAssignerAdapter implements EmployeeRoleAssigner {

  private final CreateEmployeeRoleUseCase createEmployeeRoleUseCase;

  public EmployeeRoleAssignerAdapter(CreateEmployeeRoleUseCase createEmployeeRoleUseCase) {
    this.createEmployeeRoleUseCase = createEmployeeRoleUseCase;
  }

  @Override
  public String assign(Long employeeId, Long roleId) {
    EmployeeRoleDto dto =
        createEmployeeRoleUseCase.execute(new CreateEmployeeRoleCommand(employeeId, roleId));
    return dto.role().name();
  }
}
