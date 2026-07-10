package com.vetsoftware.app.registration.application.port.in;

import com.vetsoftware.app.registration.application.command.VerifyEmailCommand;

public interface VerifyEmailUseCase {
    void execute(VerifyEmailCommand command);
}
