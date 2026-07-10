package com.vetsoftware.app.registration.application.usecase;

import com.vetsoftware.app.registration.application.command.RegisterUserCommand;
import com.vetsoftware.app.registration.application.dto.RegistrationDto;
import com.vetsoftware.app.registration.application.port.in.RegisterUserUseCase;
import com.vetsoftware.app.registration.application.port.out.CaptchaVerifier;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator;
import com.vetsoftware.app.registration.application.port.out.CompanyCreator.CompanyResult;
import com.vetsoftware.app.registration.application.port.out.CompanyIdentifierChecker;
import com.vetsoftware.app.registration.application.port.out.CompanyTaxProfileCreator;
import com.vetsoftware.app.registration.application.port.out.DefaultMembershipProvider;
import com.vetsoftware.app.registration.application.port.out.EmailVerificationTokenRepository;
import com.vetsoftware.app.registration.application.port.out.EmployeeCodeChecker;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator;
import com.vetsoftware.app.registration.application.port.out.EmployeeCreator.EmployeeResult;
import com.vetsoftware.app.registration.application.port.out.EmployeeRoleAssigner;
import com.vetsoftware.app.registration.application.port.out.BaseRoleProvider;
import com.vetsoftware.app.registration.application.port.out.BaseRoleProvider.BaseRoleData;
import com.vetsoftware.app.registration.application.port.out.RoleCreator;
import com.vetsoftware.app.registration.application.port.out.RoleCreator.RoleResult;
import com.vetsoftware.app.registration.application.port.out.RolePermissionInitializationPort;
import com.vetsoftware.app.registration.application.port.out.VerificationEmailSender;
import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final int MAX_EMPLOYEE_CODE_LENGTH = 50;
    private static final int MAX_SUFFIX_ATTEMPTS = 999;
    private static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";

    private final CaptchaVerifier captchaVerifier;
    private final CompanyCreator companyCreator;
    private final CompanyTaxProfileCreator companyTaxProfileCreator;
    private final EmployeeCreator employeeCreator;
    private final CompanyIdentifierChecker companyIdentifierChecker;
    private final EmployeeCodeChecker employeeCodeChecker;
    private final PasswordHasher passwordHasher;
    private final BaseRoleProvider baseRoleProvider;
    private final RoleCreator roleCreator;
    private final EmployeeRoleAssigner employeeRoleAssigner;
    private final DefaultMembershipProvider defaultMembershipProvider;
    private final RolePermissionInitializationPort rolePermissionInitializationPort;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final VerificationEmailSender verificationEmailSender;
    private final long verificationTtlHours;

    public RegisterUserService(CaptchaVerifier captchaVerifier,
                               CompanyCreator companyCreator,
                               CompanyTaxProfileCreator companyTaxProfileCreator,
                               EmployeeCreator employeeCreator,
                               CompanyIdentifierChecker companyIdentifierChecker,
                               EmployeeCodeChecker employeeCodeChecker,
                               PasswordHasher passwordHasher,
                               BaseRoleProvider baseRoleProvider,
                               RoleCreator roleCreator,
                               EmployeeRoleAssigner employeeRoleAssigner,
                               DefaultMembershipProvider defaultMembershipProvider,
                               RolePermissionInitializationPort rolePermissionInitializationPort,
                               EmailVerificationTokenRepository emailVerificationTokenRepository,
                               VerificationEmailSender verificationEmailSender,
                               @Value("${vetsoftware.registration.verification-token-ttl-hours:24}") long verificationTtlHours) {
        this.captchaVerifier = captchaVerifier;
        this.companyCreator = companyCreator;
        this.companyTaxProfileCreator = companyTaxProfileCreator;
        this.employeeCreator = employeeCreator;
        this.companyIdentifierChecker = companyIdentifierChecker;
        this.employeeCodeChecker = employeeCodeChecker;
        this.passwordHasher = passwordHasher;
        this.baseRoleProvider = baseRoleProvider;
        this.roleCreator = roleCreator;
        this.employeeRoleAssigner = employeeRoleAssigner;
        this.defaultMembershipProvider = defaultMembershipProvider;
        this.rolePermissionInitializationPort = rolePermissionInitializationPort;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.verificationEmailSender = verificationEmailSender;
        this.verificationTtlHours = verificationTtlHours;
    }

    @Override
    @Transactional
    public RegistrationDto execute(RegisterUserCommand command) {
        // 1) Anti-abuso: captcha antes de tocar la BD (no-op si el captcha esta deshabilitado por config).
        captchaVerifier.verify(command.recaptchaToken(), command.remoteIp());

        if (companyIdentifierChecker.exists(command.companyIdentifier())) {
            throw new IllegalArgumentException(
                    "Company identifier already in use: " + command.companyIdentifier());
        }

        String hashed = passwordHasher.hash(command.rawPassword());

        Long membershipId = defaultMembershipProvider.getDefaultMembershipId();
        CompanyResult company = companyCreator.create(
                command.companyName(),
                command.companyIdentifier(),
                command.companyAddress(),
                command.companyContactNumber(),
                command.cityId(),
                membershipId
        );

        // Identidad fiscal del emisor: toda venta (incluido el tiquete POS) la requiere. Tipo NIT, número =
        // identificador de la empresa, DV autocalculado, razón social = nombre; régimen y correo del signup.
        companyTaxProfileCreator.create(
                company.id(), command.documentType(), company.identifier(), company.name(),
                command.taxRegime(), command.fiscalEmail());

        // El dueño se crea SIN verificar (Opción B): no podrá iniciar sesión hasta confirmar su correo.
        String employeeCode = generateUniqueEmployeeCode(command.companyName(), command.employeeName());
        EmployeeResult employee = employeeCreator.create(
                employeeCode,
                hashed,
                command.employeeName(),
                command.employeeEmail(),
                company.id()
        );

        // Se instancian TODOS los base roles en la empresa (permisos filtrados por
        // la membresía). El dueño solo se auto-asigna a los mandatory (ADMIN); el
        // resto quedan como plantillas listas para asignar al personal.
        for (BaseRoleData baseRole : baseRoleProvider.findAll()) {
            RoleResult role = roleCreator.create(baseRole.name(), baseRole.code(), company.id());
            rolePermissionInitializationPort.initializeForRole(role.id(), company.id(), baseRole.id(), membershipId);
            if (baseRole.mandatory()) {
                employeeRoleAssigner.assign(employee.id(), role.id());
            }
        }

        // 2) Token de verificación de un solo uso: se guarda el HASH, se envía el valor plano por correo.
        //    Si el envío falla, la transacción hace rollback (no dejamos cuentas imposibles de verificar).
        String rawToken = VerificationTokens.generateRawToken();
        emailVerificationTokenRepository.save(EmailVerificationToken.issue(
                employee.id(), company.id(), VerificationTokens.hash(rawToken),
                LocalDateTime.now().plusHours(verificationTtlHours)));
        verificationEmailSender.send(command.employeeEmail(), command.employeeName(), rawToken);

        return new RegistrationDto(company.id(), employee.id(), command.employeeEmail(),
                STATUS_PENDING_VERIFICATION);
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
