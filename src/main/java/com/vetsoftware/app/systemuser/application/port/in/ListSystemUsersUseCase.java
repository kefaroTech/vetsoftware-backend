package com.vetsoftware.app.systemuser.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import java.util.List;

public interface ListSystemUsersUseCase {
    @RequiresPermission("admin.all")
    List<SystemUserDto> listAll(AuthContext auth);
}
