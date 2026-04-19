package com.vetsoftware.app.application.port.in;

import com.vetsoftware.app.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.application.dto.EmployeeDto;

public interface CreateEmployeeUseCase {
    EmployeeDto execute(CreateEmployeeCommand command);
}
