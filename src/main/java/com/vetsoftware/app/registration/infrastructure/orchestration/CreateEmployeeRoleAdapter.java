package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import com.vetsoftware.app.registration.application.port.out.EmployeeRoleAssigner;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CreateEmployeeRoleAdapter implements EmployeeRoleAssigner {

    private static final AuthContext SYSTEM_CONTEXT =
        new AuthContext(null, Set.of("admin.all"));

    private final CreateEmployeeRoleUseCase createEmployeeRoleUseCase;

    public CreateEmployeeRoleAdapter(CreateEmployeeRoleUseCase createEmployeeRoleUseCase) {
        this.createEmployeeRoleUseCase = createEmployeeRoleUseCase;
    }

    @Override
    public void assign(Long employeeId, Long roleId) {
        createEmployeeRoleUseCase.execute(
            new CreateEmployeeRoleCommand(employeeId, roleId),
            SYSTEM_CONTEXT
        );
    }
}
