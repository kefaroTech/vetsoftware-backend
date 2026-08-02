package com.vetsoftware.app.auth.application.port.in;

import com.vetsoftware.app.auth.application.command.LoginSystemUserCommand;
import com.vetsoftware.app.auth.application.dto.TokenDto;

public interface LoginSystemUserUseCase {
  TokenDto execute(LoginSystemUserCommand command);
}
