package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.role.application.dto.RoleDto;

public interface FindRoleUseCase {
    @RequiresPermission("admin.all")
    RoleDto findById(Long id, AuthContext auth);
}
