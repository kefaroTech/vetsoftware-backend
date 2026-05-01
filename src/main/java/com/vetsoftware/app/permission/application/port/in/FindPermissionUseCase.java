package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.permission.application.dto.PermissionDto;

public interface FindPermissionUseCase {
    @RequiresPermission("admin.all")
    PermissionDto findById(Long id, AuthContext auth);
}
