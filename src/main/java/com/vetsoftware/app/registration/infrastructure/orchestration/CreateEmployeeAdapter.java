package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.CreateEmployeeUseCase;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CreateEmployeeAdapter implements EmployeeCreator {

    private static final AuthContext SYSTEM_CONTEXT =
        new AuthContext(null, null, Set.of("admin.all"));

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final CreateEmployeeUseCase createEmployeeUseCase;

    public CreateEmployeeAdapter(CreateEmployeeUseCase createEmployeeUseCase) {
        this.createEmployeeUseCase = createEmployeeUseCase;
    }

    @Override
    public EmployeeResult create(String employeeCode, String hashedPassword, String name,
                                 String email, Long companyId) {
        EmployeeDto dto = createEmployeeUseCase.execute(
            new CreateEmployeeCommand(employeeCode, hashedPassword, name, email, DEFAULT_STATUS, companyId),
            SYSTEM_CONTEXT
        );
        return new EmployeeResult(dto.id());
    }
}
