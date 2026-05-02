package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.role.application.dto.RoleDto;
import java.util.List;

public interface ListRolesUseCase {
    @RequiresPermission("admin.all")
    List<RoleDto> listAll(AuthContext auth);
}
