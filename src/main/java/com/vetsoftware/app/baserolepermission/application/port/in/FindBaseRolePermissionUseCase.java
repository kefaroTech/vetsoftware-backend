package com.vetsoftware.app.baserolepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;

public interface FindBaseRolePermissionUseCase {
    @RequiresPermission("admin.all")
    BaseRolePermissionDto findById(Long id, AuthContext auth);
}
