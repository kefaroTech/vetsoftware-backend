package com.vetsoftware.app.systemuser.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;

public interface FindSystemUserUseCase {
    @RequiresPermission("admin.all")
    SystemUserDto findById(Long id, AuthContext auth);
}
