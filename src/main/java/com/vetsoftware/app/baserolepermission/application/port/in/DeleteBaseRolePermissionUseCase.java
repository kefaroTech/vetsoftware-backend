package com.vetsoftware.app.baserolepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteBaseRolePermissionUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
