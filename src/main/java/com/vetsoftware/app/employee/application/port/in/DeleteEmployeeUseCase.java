package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteEmployeeUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
