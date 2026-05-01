package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.permission.application.dto.PermissionDto;
import java.util.List;

public interface ListPermissionsUseCase {
    @RequiresPermission("admin.all")
    List<PermissionDto> listAll(AuthContext auth);
}
