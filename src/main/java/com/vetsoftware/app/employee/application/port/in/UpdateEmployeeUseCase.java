package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.command.UpdateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;

public interface UpdateEmployeeUseCase {
    EmployeeDto execute(UpdateEmployeeCommand command);
}
