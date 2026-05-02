package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;

public interface FindEmployeeRoleUseCase {
    @RequiresPermission("admin.all")
    EmployeeRoleDto findById(Long id, AuthContext auth);
}
