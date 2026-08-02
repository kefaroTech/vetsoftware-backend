package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.port.in.CreateEmployeeUseCase;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator;
import org.springframework.stereotype.Component;

@Component
public class CreateEmployeeAdapter implements EmployeeCreator {

  private final CreateEmployeeUseCase createEmployeeUseCase;
  private final SystemAuthRunner systemAuthRunner;

  public CreateEmployeeAdapter(
      CreateEmployeeUseCase createEmployeeUseCase, SystemAuthRunner systemAuthRunner) {
    this.createEmployeeUseCase = createEmployeeUseCase;
    this.systemAuthRunner = systemAuthRunner;
  }

  @Override
  public EmployeeResult create(
      String employeeCode, String rawPassword, String name, String email, Long companyId) {
    // Auto-registro público: el dueño se crea SIN verificar (no podrá iniciar sesión hasta
    // confirmar el
    // correo — Opción B). La contraseña va CRUDA; CreateEmployeeService la hashea una sola vez.
    var dto =
        systemAuthRunner.call(
            () ->
                createEmployeeUseCase.execute(
                    new CreateEmployeeCommand(
                        employeeCode, rawPassword, name, email, companyId, false)));
    return new EmployeeResult(dto.id());
  }
}
