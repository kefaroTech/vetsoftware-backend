package com.vetsoftware.app.basepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import java.util.List;

public interface ListBasePermissionsUseCase {
    @RequiresPermission("admin.all")
    List<BasePermissionDto> listAll(AuthContext auth);
}
