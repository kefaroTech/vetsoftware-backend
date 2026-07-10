package com.vetsoftware.app.passwordreset.infrastructure.orchestration;

import com.vetsoftware.app.employee.application.command.ResetEmployeePasswordCommand;
import com.vetsoftware.app.employee.application.port.in.ResetEmployeePasswordUseCase;
import com.vetsoftware.app.passwordreset.application.port.out.EmployeePasswordResetter;
import org.springframework.stereotype.Component;

@Component
public class EmployeePasswordResetterAdapter implements EmployeePasswordResetter {

    private final ResetEmployeePasswordUseCase resetEmployeePasswordUseCase;

    public EmployeePasswordResetterAdapter(ResetEmployeePasswordUseCase resetEmployeePasswordUseCase) {
        this.resetEmployeePasswordUseCase = resetEmployeePasswordUseCase;
    }

    @Override
    public void reset(Long employeeId, String rawNewPassword) {
        resetEmployeePasswordUseCase.execute(new ResetEmployeePasswordCommand(employeeId, rawNewPassword));
    }
}
