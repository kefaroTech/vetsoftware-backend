package com.vetsoftware.app.registration.application.usecase;

import com.vetsoftware.app.registration.application.command.RegisterUserCommand;
import com.vetsoftware.app.registration.application.dto.RegistrationDto;
import com.vetsoftware.app.registration.application.port.in.RegisterUserUseCase;
import com.vetsoftware.app.registration.application.port.out.BaseRoleProvider;
import com.vetsoftware.app.registration.application.port.out.BaseRoleProvider.BaseRoleData;
import com.vetsoftware.app.registration.application.port.out.BranchCreator;
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
import com.vetsoftware.app.registration.application.port.out.RoleCreator;
import com.vetsoftware.app.registration.application.port.out.RoleCreator.RoleResult;
import com.vetsoftware.app.registration.application.port.out.RolePermissionInitializationPort;
import com.vetsoftware.app.registration.application.port.out.VerificationEmailSender;
import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import com.vetsoftware.app.registration.domain.EmployeeCodeAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "registration.register")
@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";

    private final CaptchaVerifier captchaVerifier;
    private final CompanyCreator companyCreator;
    private final CompanyTaxProfileCreator companyTaxProfileCreator;
    private final BranchCreator branchCreator;
    private final EmployeeCreator employeeCreator;
    private final CompanyIdentifierChecker companyIdentifierChecker;
    private final EmployeeCodeChecker employeeCodeChecker;
    private final BaseRoleProvider baseRoleProvider;
    private final RoleCreator roleCreator;
    private final EmployeeRoleAssigner employeeRoleAssigner;
    private final DefaultMembershipProvider defaultMembershipProvider;
    private final RolePermissionInitializationPort rolePermissionInitializationPort;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final VerificationEmailSender verificationEmailSender;
    private final long verificationTtlHours;

    public RegisterUserService(CaptchaVerifier captchaVerifier, CompanyCreator companyCreator,
            CompanyTaxProfileCreator companyTaxProfileCreator, BranchCreator branchCreator,
            EmployeeCreator employeeCreator, CompanyIdentifierChecker companyIdentifierChecker,
            EmployeeCodeChecker employeeCodeChecker, BaseRoleProvider baseRoleProvider,
            RoleCreator roleCreator, EmployeeRoleAssigner employeeRoleAssigner,
            DefaultMembershipProvider defaultMembershipProvider,
            RolePermissionInitializationPort rolePermissionInitializationPort,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            VerificationEmailSender verificationEmailSender,
            @Value("${vetsoftware.registration.verification-token-ttl-hours:24}") long verificationTtlHours) {
        this.captchaVerifier = captchaVerifier;
        this.companyCreator = companyCreator;
        this.companyTaxProfileCreator = companyTaxProfileCreator;
        this.branchCreator = branchCreator;
        this.employeeCreator = employeeCreator;
        this.companyIdentifierChecker = companyIdentifierChecker;
        this.employeeCodeChecker = employeeCodeChecker;
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
        // 1) Anti-abuso: captcha antes de tocar la BD (no-op si el captcha esta
        // deshabilitado por
        // config).
        captchaVerifier.verify(command.recaptchaToken(), command.remoteIp());

        if (companyIdentifierChecker.exists(command.companyIdentifier())) {
            throw new IllegalArgumentException(
                    "Company identifier already in use: " + command.companyIdentifier());
        }

        Long membershipId = defaultMembershipProvider.getDefaultMembershipId();
        CompanyResult company = companyCreator.create(command.companyName(),
                command.companyIdentifier(), command.companyAddress(),
                command.companyContactNumber(), command.cityId(), membershipId);

        // Identidad fiscal del emisor: toda venta (incluido el tiquete POS) la
        // requiere. Tipo NIT,
        // número =
        // identificador de la empresa, DV autocalculado, razón social = nombre; régimen
        // y correo del
        // signup.
        companyTaxProfileCreator.create(company.id(), command.documentType(), company.identifier(),
                company.name(), command.taxRegime(), command.fiscalEmail());

        // Multi-sucursal: toda empresa nace con su sede "Principal" (invariante: ≥1
        // sede por empresa).
        // Hereda ciudad/dirección/teléfono del registro, igual que el backfill de las
        // empresas
        // existentes.
        branchCreator.create("Principal", "PRINCIPAL", command.companyAddress(),
                command.companyContactNumber(), command.cityId(), company.id());

        // El dueño ELIGE su código de acceso (Opción A). Revalidamos aquí que siga
        // libre (defensa ante
        // la
        // El usuario de acceso ES el correo del administrador (un email = una
        // veterinaria). Como
        // employee_code
        // es único global, si el correo ya está registrado se rechaza aquí. El dueño se
        // crea SIN
        // verificar (Opción B).
        String employeeCode = command.employeeEmail().trim();
        if (employeeCodeChecker.exists(employeeCode)) {
            throw new EmployeeCodeAlreadyExistsException(employeeCode);
        }
        // Se pasa la contraseña CRUDA: CreateEmployeeService la hashea una sola vez
        // (igual que el alta
        // de
        // staff por el admin). No pre-hashear aquí, o quedaría doble-hasheada y el
        // login nunca haría
        // match.
        EmployeeResult employee = employeeCreator.create(employeeCode, command.rawPassword(),
                command.employeeName(), command.employeeEmail(), company.id());

        // Se instancian TODOS los base roles en la empresa (permisos filtrados por
        // la membresía). El dueño solo se auto-asigna a los mandatory (ADMIN); el
        // resto quedan como plantillas listas para asignar al personal.
        for (BaseRoleData baseRole : baseRoleProvider.findAll()) {
            RoleResult role = roleCreator.create(baseRole.name(), baseRole.code(), company.id());
            rolePermissionInitializationPort.initializeForRole(role.id(), company.id(),
                    baseRole.id(), membershipId);
            if (baseRole.mandatory()) {
                employeeRoleAssigner.assign(employee.id(), role.id());
            }
        }

        // 2) Token de verificación de un solo uso: se guarda el HASH, se envía el valor
        // plano por
        // correo.
        // El envío NO bloquea el registro (best-effort).
        String rawToken = VerificationTokens.generateRawToken();
        emailVerificationTokenRepository.save(EmailVerificationToken.issue(employee.id(),
                company.id(), VerificationTokens.hash(rawToken),
                LocalDateTime.now().plusHours(verificationTtlHours)));
        verificationEmailSender.send(command.employeeEmail(), command.employeeName(),
                command.companyName(), rawToken);

        return new RegistrationDto(company.id(), employee.id(), command.employeeEmail(),
                STATUS_PENDING_VERIFICATION);
    }
}
