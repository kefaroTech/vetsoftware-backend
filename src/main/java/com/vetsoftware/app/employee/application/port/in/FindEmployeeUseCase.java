package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;

public interface FindEmployeeUseCase {
    @RequiresPermission("admin.all")
    EmployeeDto findById(Long id, AuthContext auth);
}
