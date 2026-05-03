package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.owner.application.command.UpdateOwnerCommand;
import com.vetsoftware.app.owner.application.dto.OwnerDto;

public interface UpdateOwnerUseCase {
    @RequiresPermission("admin.all")
    OwnerDto execute(UpdateOwnerCommand command, AuthContext auth);
}
