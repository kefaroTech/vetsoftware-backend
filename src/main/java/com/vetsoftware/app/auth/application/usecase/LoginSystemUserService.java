package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.command.LoginSystemUserCommand;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.auth.application.port.in.LoginSystemUserUseCase;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenIssuer;
import com.vetsoftware.app.auth.application.port.out.SystemUserCredentialsRepository;
import com.vetsoftware.app.auth.application.port.out.SystemUserCredentialsRepository.SystemUserCredentials;
import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginSystemUserService implements LoginSystemUserUseCase {

    private final SystemUserCredentialsRepository credentialsRepository;
    private final TokenGenerator tokenGenerator;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final PasswordHasher passwordHasher;

    public LoginSystemUserService(SystemUserCredentialsRepository credentialsRepository,
                                  TokenGenerator tokenGenerator,
                                  RefreshTokenIssuer refreshTokenIssuer,
                                  PasswordHasher passwordHasher) {
        this.credentialsRepository = credentialsRepository;
        this.tokenGenerator = tokenGenerator;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public TokenDto execute(LoginSystemUserCommand command) {
        SystemUserCredentials credentials = credentialsRepository.findByCode(command.code())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(command.password(), credentials.hashPassword()))
            throw new InvalidCredentialsException();

        String accessToken = tokenGenerator.generate(credentials.id(), "SYSTEM_USER", null, null);
        String refreshToken = refreshTokenIssuer.issue(credentials.id(), "SYSTEM_USER");
        return new TokenDto(accessToken, "SYSTEM_USER", refreshToken);
    }
}
