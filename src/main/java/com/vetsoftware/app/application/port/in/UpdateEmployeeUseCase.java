package com.vetsoftware.app.application.port.in;

import com.vetsoftware.app.application.command.UpdateEmployeeCommand;
import com.vetsoftware.app.application.dto.EmployeeDto;

public interface UpdateEmployeeUseCase {
    EmployeeDto execute(UpdateEmployeeCommand command);
}
