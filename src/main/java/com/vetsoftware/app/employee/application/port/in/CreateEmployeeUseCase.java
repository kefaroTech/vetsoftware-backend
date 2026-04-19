package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;

public interface CreateEmployeeUseCase {
    EmployeeDto execute(CreateEmployeeCommand command);
}
