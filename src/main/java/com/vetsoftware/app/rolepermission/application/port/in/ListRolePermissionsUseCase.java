package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import java.util.List;

public interface ListRolePermissionsUseCase {
    @RequiresPermission("admin.all")
    List<RolePermissionDto> listAll(AuthContext auth);
}
