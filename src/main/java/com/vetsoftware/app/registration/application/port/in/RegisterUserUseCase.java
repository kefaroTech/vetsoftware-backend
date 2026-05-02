package com.vetsoftware.app.registration.application.port.in;

import com.vetsoftware.app.registration.application.command.RegisterUserCommand;
import com.vetsoftware.app.registration.application.dto.RegistrationDto;

public interface RegisterUserUseCase {
    RegistrationDto execute(RegisterUserCommand command);
}
