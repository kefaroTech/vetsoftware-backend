package com.vetsoftware.app.systempermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;

public interface FindSystemPermissionUseCase {
    @RequiresPermission("admin.all")
    SystemPermissionDto findById(Long id, AuthContext auth);
}
