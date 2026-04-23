package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employee.application.command.UpdateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;

public interface UpdateEmployeeUseCase {
    @RequiresPermission("admin.all")
    EmployeeDto execute(UpdateEmployeeCommand command, AuthContext auth);
}
