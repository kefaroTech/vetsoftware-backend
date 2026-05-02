package com.vetsoftware.app.systemuserpermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import java.util.List;

public interface ListSystemUserPermissionsUseCase {
    @RequiresPermission("admin.all")
    List<SystemUserPermissionDto> listAll(AuthContext auth);
}
