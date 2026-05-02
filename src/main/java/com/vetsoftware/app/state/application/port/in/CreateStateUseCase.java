package com.vetsoftware.app.state.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.state.application.command.CreateStateCommand;
import com.vetsoftware.app.state.application.dto.StateDto;

public interface CreateStateUseCase {
    @RequiresPermission({"admin.all"})
    StateDto execute(CreateStateCommand command, AuthContext auth);
}
