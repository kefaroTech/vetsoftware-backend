package com.vetsoftware.app.owner.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.owner.application.command.CreateOwnerCommand;
import com.vetsoftware.app.owner.application.dto.OwnerDto;

public interface CreateOwnerUseCase {
    @RequiresPermission("admin.all")
    OwnerDto execute(CreateOwnerCommand command, AuthContext auth);
}
