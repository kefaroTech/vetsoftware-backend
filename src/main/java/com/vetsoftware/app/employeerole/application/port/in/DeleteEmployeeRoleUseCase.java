package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteEmployeeRoleUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
