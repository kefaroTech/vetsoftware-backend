package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteRolePermissionUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
