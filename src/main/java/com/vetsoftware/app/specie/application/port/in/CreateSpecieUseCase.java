package com.vetsoftware.app.specie.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.specie.application.command.CreateSpecieCommand;
import com.vetsoftware.app.specie.application.dto.SpecieDto;

public interface CreateSpecieUseCase {
    @RequiresPermission("admin.all")
    SpecieDto execute(CreateSpecieCommand command, AuthContext auth);
}
