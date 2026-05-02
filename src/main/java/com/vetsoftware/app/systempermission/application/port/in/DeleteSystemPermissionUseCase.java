package com.vetsoftware.app.systempermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteSystemPermissionUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
