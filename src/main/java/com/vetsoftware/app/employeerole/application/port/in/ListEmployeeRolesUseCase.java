package com.vetsoftware.app.employeerole.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import java.util.List;

public interface ListEmployeeRolesUseCase {
    @RequiresPermission("admin.all")
    List<EmployeeRoleDto> listAll(AuthContext auth);
}
