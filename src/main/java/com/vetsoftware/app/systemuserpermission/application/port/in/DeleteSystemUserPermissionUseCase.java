package com.vetsoftware.app.systemuserpermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteSystemUserPermissionUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
