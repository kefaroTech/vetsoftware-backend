package com.vetsoftware.app.submodule.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import java.util.List;

public interface ListSubModulesUseCase {
    @RequiresPermission("admin.all")
    List<SubModuleDto> listAll(AuthContext auth);
}
