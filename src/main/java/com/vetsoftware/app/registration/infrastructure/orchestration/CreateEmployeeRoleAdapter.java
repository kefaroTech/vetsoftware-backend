package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import com.vetsoftware.app.registration.application.port.out.EmployeeRoleAssigner;
import org.springframework.stereotype.Component;

@Component
public class CreateEmployeeRoleAdapter implements EmployeeRoleAssigner {

    private final CreateEmployeeRoleUseCase createEmployeeRoleUseCase;

    public CreateEmployeeRoleAdapter(CreateEmployeeRoleUseCase createEmployeeRoleUseCase) {
        this.createEmployeeRoleUseCase = createEmployeeRoleUseCase;
    }

    @Override
    public void assign(Long employeeId, Long roleId) {
        createEmployeeRoleUseCase.execute(
            new CreateEmployeeRoleCommand(employeeId, roleId),
            SystemContext.INSTANCE
        );
    }
}
