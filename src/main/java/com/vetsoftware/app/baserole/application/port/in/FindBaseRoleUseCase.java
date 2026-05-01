package com.vetsoftware.app.baserole.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;

public interface FindBaseRoleUseCase {
    @RequiresPermission("admin.all")
    BaseRoleDto findById(Long id, AuthContext auth);
}
