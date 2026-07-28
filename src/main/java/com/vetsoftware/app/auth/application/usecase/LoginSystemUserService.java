package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.command.LoginSystemUserCommand;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.auth.application.port.in.LoginSystemUserUseCase;
import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository;
import com.vetsoftware.app.auth.application.port.out.AuthSystemUserRepository.AuthSystemUser;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenIssuer;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository;
import com.vetsoftware.app.auth.application.port.out.SystemUserCredentialsRepository;
import com.vetsoftware.app.auth.application.port.out.SystemUserCredentialsRepository.SystemUserCredentials;
import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "auth.loginSystemUser")
@Service
public class LoginSystemUserService implements LoginSystemUserUseCase {

    private final SystemUserCredentialsRepository credentialsRepository;
    private final TokenGenerator tokenGenerator;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final PasswordHasher passwordHasher;
    private final AuthSystemUserRepository authSystemUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public LoginSystemUserService(SystemUserCredentialsRepository credentialsRepository,
                                  TokenGenerator tokenGenerator,
                                  RefreshTokenIssuer refreshTokenIssuer,
                                  PasswordHasher passwordHasher,
                                  AuthSystemUserRepository authSystemUserRepository,
                                  RefreshTokenRepository refreshTokenRepository) {
        this.credentialsRepository = credentialsRepository;
        this.tokenGenerator = tokenGenerator;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.passwordHasher = passwordHasher;
        this.authSystemUserRepository = authSystemUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public TokenDto execute(LoginSystemUserCommand command) {
        SystemUserCredentials credentials = credentialsRepository.findByCode(command.code())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(command.password(), credentials.hashPassword()))
            throw new InvalidCredentialsException();

        AuthSystemUser activeSession = authSystemUserRepository.rotateAuthVersion(credentials.id())
                .orElseThrow(InvalidCredentialsException::new);
        refreshTokenRepository.revokeAllForSubject(credentials.id(), "SYSTEM_USER");

        String accessToken = tokenGenerator.generate(
                activeSession.id(), "SYSTEM_USER", null, activeSession.authVersion());
        String refreshToken = refreshTokenIssuer.issue(
                activeSession.id(), "SYSTEM_USER", activeSession.authVersion());
        return new TokenDto(accessToken, "SYSTEM_USER", refreshToken);
    }
}
