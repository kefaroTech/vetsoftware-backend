package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;

public interface CreateEmployeeRoleUseCase {
    @RequiresPermission("admin.all")
    EmployeeRoleDto execute(CreateEmployeeRoleCommand command, AuthContext auth);
}
