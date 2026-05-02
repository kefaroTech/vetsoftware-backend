package com.vetsoftware.app.registration.application.usecase;

import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import com.vetsoftware.app.registration.application.command.RegisterUserCommand;
import com.vetsoftware.app.registration.application.dto.RegistrationDto;
import com.vetsoftware.app.registration.application.port.in.RegisterUserUseCase;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator.CompanyResult;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator.EmployeeResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private final CompanyCreator companyCreator;
    private final EmployeeCreator employeeCreator;
    private final PasswordHasher passwordHasher;
    private final TokenGenerator tokenGenerator;

    public RegisterUserService(CompanyCreator companyCreator,
                               EmployeeCreator employeeCreator,
                               PasswordHasher passwordHasher,
                               TokenGenerator tokenGenerator) {
        this.companyCreator = companyCreator;
        this.employeeCreator = employeeCreator;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    @Transactional
    public RegistrationDto execute(RegisterUserCommand command) {
        String hashed = passwordHasher.hash(command.rawPassword());

        CompanyResult company = companyCreator.create(
            command.companyName(),
            command.companyIdentifier(),
            command.companyAddress(),
            command.companyContactNumber()
        );

        EmployeeResult employee = employeeCreator.create(
            command.employeeCode(),
            hashed,
            command.employeeName(),
            command.employeeEmail(),
            company.id()
        );

        String token = tokenGenerator.generate(employee.id(), "EMPLOYEE");
        return new RegistrationDto(company.id(), employee.id(), token, "EMPLOYEE");
    }
}
