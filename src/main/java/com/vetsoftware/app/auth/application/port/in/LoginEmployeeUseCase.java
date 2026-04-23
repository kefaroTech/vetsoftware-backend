package com.vetsoftware.app.auth.application.port.in;

import com.vetsoftware.app.auth.application.command.LoginEmployeeCommand;
import com.vetsoftware.app.auth.application.dto.TokenDto;

public interface LoginEmployeeUseCase {
    TokenDto execute(LoginEmployeeCommand command);
}
