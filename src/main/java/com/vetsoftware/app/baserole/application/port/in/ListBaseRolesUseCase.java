package com.vetsoftware.app.baserole.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import java.util.List;

public interface ListBaseRolesUseCase {
    @RequiresPermission("admin.all")
    List<BaseRoleDto> listAll(AuthContext auth);
}
