package com.vetsoftware.app.systempermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import java.util.List;

public interface ListSystemPermissionsUseCase {
    @RequiresPermission("admin.all")
    List<SystemPermissionDto> listAll(AuthContext auth);
}
