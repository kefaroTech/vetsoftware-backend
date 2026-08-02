package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import com.vetsoftware.app.registration.application.port.out.EmployeeRoleAssigner;
import org.springframework.stereotype.Component;

@Component
public class CreateEmployeeRoleAdapter implements EmployeeRoleAssigner {

    private final CreateEmployeeRoleUseCase createEmployeeRoleUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public CreateEmployeeRoleAdapter(CreateEmployeeRoleUseCase createEmployeeRoleUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.createEmployeeRoleUseCase = createEmployeeRoleUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void assign(Long employeeId, Long roleId) {
        systemAuthRunner.run(() -> createEmployeeRoleUseCase
                .execute(new CreateEmployeeRoleCommand(employeeId, roleId)));
    }
}
