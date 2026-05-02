package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteRoleUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
