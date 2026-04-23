package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.command.LoginSystemUserCommand;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.auth.application.port.in.LoginSystemUserUseCase;
import com.vetsoftware.app.auth.application.port.out.SystemUserCredentialsRepository;
import com.vetsoftware.app.auth.application.port.out.SystemUserCredentialsRepository.SystemUserCredentials;
import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import org.springframework.stereotype.Service;

@Service
public class LoginSystemUserService implements LoginSystemUserUseCase {

    private final SystemUserCredentialsRepository credentialsRepository;
    private final TokenGenerator tokenGenerator;

    public LoginSystemUserService(SystemUserCredentialsRepository credentialsRepository,
                                  TokenGenerator tokenGenerator) {
        this.credentialsRepository = credentialsRepository;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public TokenDto execute(LoginSystemUserCommand command) {
        SystemUserCredentials credentials = credentialsRepository.findByCode(command.code())
                .orElseThrow(InvalidCredentialsException::new);

        if (!credentials.hashPassword().equals(command.password()))
            throw new InvalidCredentialsException();

        return new TokenDto(tokenGenerator.generate(credentials.id(), "SYSTEM_USER"), "SYSTEM_USER");
    }
}
