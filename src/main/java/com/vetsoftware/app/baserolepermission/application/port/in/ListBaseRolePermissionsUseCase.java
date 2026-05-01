package com.vetsoftware.app.baserolepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import java.util.List;

public interface ListBaseRolePermissionsUseCase {
    @RequiresPermission("admin.all")
    List<BaseRolePermissionDto> listAll(AuthContext auth);
}
