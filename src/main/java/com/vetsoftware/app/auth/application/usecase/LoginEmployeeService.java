package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.command.LoginEmployeeCommand;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.auth.application.port.in.LoginEmployeeUseCase;
import com.vetsoftware.app.auth.application.port.out.EmployeeCredentialsRepository;
import com.vetsoftware.app.auth.application.port.out.EmployeeCredentialsRepository.EmployeeCredentials;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenIssuer;
import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginEmployeeService implements LoginEmployeeUseCase {

    private final EmployeeCredentialsRepository credentialsRepository;
    private final TokenGenerator tokenGenerator;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final PasswordHasher passwordHasher;

    public LoginEmployeeService(EmployeeCredentialsRepository credentialsRepository,
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
    public TokenDto execute(LoginEmployeeCommand command) {
        EmployeeCredentials credentials = credentialsRepository.findByCode(command.employeeCode())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(command.password(), credentials.hashPassword()))
            throw new InvalidCredentialsException();

        String accessToken = tokenGenerator.generate(
                credentials.id(), "EMPLOYEE", credentials.companyId(), credentials.authVersion());
        String refreshToken = refreshTokenIssuer.issue(credentials.id(), "EMPLOYEE");
        return new TokenDto(accessToken, "EMPLOYEE", refreshToken);
    }
}
