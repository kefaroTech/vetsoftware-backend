package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employee.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;

public interface CreateEmployeeUseCase {
    @RequiresPermission("admin.all")
    EmployeeDto execute(CreateEmployeeCommand command, AuthContext auth);
}
