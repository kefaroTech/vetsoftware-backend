package com.vetsoftware.app.systemuserpermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;

public interface FindSystemUserPermissionUseCase {
    @RequiresPermission("admin.all")
    SystemUserPermissionDto findById(Long id, AuthContext auth);
}
