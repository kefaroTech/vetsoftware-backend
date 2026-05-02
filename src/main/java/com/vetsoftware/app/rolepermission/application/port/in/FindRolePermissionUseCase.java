package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;

public interface FindRolePermissionUseCase {
    @RequiresPermission("admin.all")
    RolePermissionDto findById(Long id, AuthContext auth);
}
