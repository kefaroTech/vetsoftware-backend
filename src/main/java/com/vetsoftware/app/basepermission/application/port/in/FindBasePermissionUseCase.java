package com.vetsoftware.app.basepermission.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;

public interface FindBasePermissionUseCase {
    @RequiresPermission("admin.all")
    BasePermissionDto findById(Long id, AuthContext auth);
}
