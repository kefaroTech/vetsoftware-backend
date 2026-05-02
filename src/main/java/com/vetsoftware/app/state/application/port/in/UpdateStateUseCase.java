package com.vetsoftware.app.state.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.state.application.command.UpdateStateCommand;
import com.vetsoftware.app.state.application.dto.StateDto;

public interface UpdateStateUseCase {
    @RequiresPermission({"admin.all"})
    StateDto execute(UpdateStateCommand command, AuthContext auth);
}
