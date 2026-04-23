package com.vetsoftware.app.basepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;

public interface DeleteBasePermissionUseCase {
    @RequiresPermission("admin.all")
    void execute(Long id, AuthContext auth);
}
