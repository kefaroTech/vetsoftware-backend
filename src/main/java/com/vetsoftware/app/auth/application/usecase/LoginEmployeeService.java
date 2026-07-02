package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.command.LoginEmployeeCommand;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.auth.application.port.in.LoginEmployeeUseCase;
import com.vetsoftware.app.auth.application.port.out.EmployeeCredentialsRepository;
import com.vetsoftware.app.auth.application.port.out.EmployeeCredentialsRepository.EmployeeCredentials;
import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import org.springframework.stereotype.Service;

@Service
public class LoginEmployeeService implements LoginEmployeeUseCase {

    private final EmployeeCredentialsRepository credentialsRepository;
    private final TokenGenerator tokenGenerator;
    private final PasswordHasher passwordHasher;

    public LoginEmployeeService(EmployeeCredentialsRepository credentialsRepository,
                                TokenGenerator tokenGenerator,
                                PasswordHasher passwordHasher) {
        this.credentialsRepository = credentialsRepository;
        this.tokenGenerator = tokenGenerator;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public TokenDto execute(LoginEmployeeCommand command) {
        EmployeeCredentials credentials = credentialsRepository.findByCode(command.employeeCode())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(command.password(), credentials.hashPassword()))
            throw new InvalidCredentialsException();

        return new TokenDto(tokenGenerator.generate(
            credentials.id(), "EMPLOYEE", credentials.companyId(), credentials.authVersion()), "EMPLOYEE");
    }
}
