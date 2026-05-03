package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.port.in.CreateEmployeeUseCase;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator;
import org.springframework.stereotype.Component;

@Component
public class CreateEmployeeAdapter implements EmployeeCreator {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final CreateEmployeeUseCase createEmployeeUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public CreateEmployeeAdapter(CreateEmployeeUseCase createEmployeeUseCase,
                                 SystemAuthRunner systemAuthRunner) {
        this.createEmployeeUseCase = createEmployeeUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public EmployeeResult create(String employeeCode, String hashedPassword, String name,
                                 String email, Long companyId) {
        var dto = systemAuthRunner.call(() -> createEmployeeUseCase.execute(
            new CreateEmployeeCommand(employeeCode, hashedPassword, name, email, DEFAULT_STATUS, companyId)
        ));
        return new EmployeeResult(dto.id());
    }
}
