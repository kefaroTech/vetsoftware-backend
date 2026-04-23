package com.vetsoftware.app.auth.application.usecase;

import com.vetsoftware.app.auth.application.command.LoginEmployeeCommand;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
import com.vetsoftware.app.auth.application.port.in.LoginEmployeeUseCase;
import com.vetsoftware.app.auth.application.port.out.EmployeeCredentialsRepository;
import com.vetsoftware.app.auth.application.port.out.EmployeeCredentialsRepository.EmployeeCredentials;
import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import org.springframework.stereotype.Service;

@Service
public class LoginEmployeeService implements LoginEmployeeUseCase {

    private final EmployeeCredentialsRepository credentialsRepository;
    private final TokenGenerator tokenGenerator;

    public LoginEmployeeService(EmployeeCredentialsRepository credentialsRepository,
                                TokenGenerator tokenGenerator) {
        this.credentialsRepository = credentialsRepository;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public TokenDto execute(LoginEmployeeCommand command) {
        EmployeeCredentials credentials = credentialsRepository.findByCode(command.employeeCode())
                .orElseThrow(InvalidCredentialsException::new);

        if (!credentials.hashPassword().equals(command.password()))
            throw new InvalidCredentialsException();

        return new TokenDto(tokenGenerator.generate(credentials.id(), "EMPLOYEE"), "EMPLOYEE");
    }
}
