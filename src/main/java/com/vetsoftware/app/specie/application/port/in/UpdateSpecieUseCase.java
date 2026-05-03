package com.vetsoftware.app.specie.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.specie.application.command.UpdateSpecieCommand;
import com.vetsoftware.app.specie.application.dto.SpecieDto;

public interface UpdateSpecieUseCase {
    @RequiresPermission("admin.all")
    SpecieDto execute(UpdateSpecieCommand command, AuthContext auth);
}
