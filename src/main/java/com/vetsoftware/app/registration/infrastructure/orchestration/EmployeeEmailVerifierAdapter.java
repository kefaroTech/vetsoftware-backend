package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.employee.application.port.in.VerifyEmployeeEmailUseCase;
import com.vetsoftware.app.registration.application.port.out.EmployeeEmailVerifier;
import org.springframework.stereotype.Component;

@Component
public class EmployeeEmailVerifierAdapter implements EmployeeEmailVerifier {

    private final VerifyEmployeeEmailUseCase verifyEmployeeEmailUseCase;

    public EmployeeEmailVerifierAdapter(VerifyEmployeeEmailUseCase verifyEmployeeEmailUseCase) {
        this.verifyEmployeeEmailUseCase = verifyEmployeeEmailUseCase;
    }

    @Override
    public void verify(Long employeeId) {
        verifyEmployeeEmailUseCase.execute(employeeId);
    }
}
