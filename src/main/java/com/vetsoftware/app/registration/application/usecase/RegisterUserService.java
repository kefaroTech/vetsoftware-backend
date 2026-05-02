package com.vetsoftware.app.registration.application.usecase;

import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import com.vetsoftware.app.registration.application.command.RegisterUserCommand;
import com.vetsoftware.app.registration.application.dto.RegistrationDto;
import com.vetsoftware.app.registration.application.port.in.RegisterUserUseCase;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator.CompanyResult;
import com.vetsoftware.app.registration.application.port.out.CompanyIdentifierChecker;
import com.vetsoftware.app.registration.application.port.out.EmployeeCodeChecker;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator.EmployeeResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final int MAX_EMPLOYEE_CODE_LENGTH = 50;
    private static final int MAX_SUFFIX_ATTEMPTS = 999;

    private final CompanyCreator companyCreator;
    private final EmployeeCreator employeeCreator;
    private final CompanyIdentifierChecker companyIdentifierChecker;
    private final EmployeeCodeChecker employeeCodeChecker;
    private final PasswordHasher passwordHasher;
    private final TokenGenerator tokenGenerator;

    public RegisterUserService(CompanyCreator companyCreator,
                               EmployeeCreator employeeCreator,
                               CompanyIdentifierChecker companyIdentifierChecker,
                               EmployeeCodeChecker employeeCodeChecker,
                               PasswordHasher passwordHasher,
                               TokenGenerator tokenGenerator) {
        this.companyCreator = companyCreator;
        this.employeeCreator = employeeCreator;
        this.companyIdentifierChecker = companyIdentifierChecker;
        this.employeeCodeChecker = employeeCodeChecker;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    @Transactional
    public RegistrationDto execute(RegisterUserCommand command) {
        if (companyIdentifierChecker.exists(command.companyIdentifier())) {
            throw new IllegalArgumentException(
                "Company identifier already in use: " + command.companyIdentifier());
        }

        String hashed = passwordHasher.hash(command.rawPassword());

        CompanyResult company = companyCreator.create(
            command.companyName(),
            command.companyIdentifier(),
            command.companyAddress(),
            command.companyContactNumber(),
            command.cityId()
        );

        String employeeCode = generateUniqueEmployeeCode(command.companyName(), command.employeeName());
        EmployeeResult employee = employeeCreator.create(
            employeeCode,
            hashed,
            command.employeeName(),
            command.employeeEmail(),
            company.id()
        );

        String token = tokenGenerator.generate(employee.id(), "EMPLOYEE");
        return new RegistrationDto(company.id(), employee.id(), token, "EMPLOYEE");
    }

    private String generateUniqueEmployeeCode(String companyName, String employeeName) {
        String base = EmployeeCodeGenerator.generate(companyName, employeeName);
        if (!employeeCodeChecker.exists(base)) return base;
        for (int i = 2; i <= MAX_SUFFIX_ATTEMPTS; i++) {
            String candidate = withSuffix(base, i);
            if (!employeeCodeChecker.exists(candidate)) return candidate;
        }
        throw new IllegalStateException(
            "Could not generate unique employee code after " + MAX_SUFFIX_ATTEMPTS + " attempts");
    }

    private static String withSuffix(String base, int n) {
        String suffix = "-" + n;
        int reserved = MAX_EMPLOYEE_CODE_LENGTH - suffix.length();
        String trimmed = base.length() > reserved ? base.substring(0, reserved) : base;
        return trimmed + suffix;
    }
}
