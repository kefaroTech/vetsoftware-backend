package com.vetsoftware.app.registration.infrastructure.web;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.registration.application.command.RegisterUserCommand;
import com.vetsoftware.app.registration.application.command.VerifyEmailCommand;
import com.vetsoftware.app.registration.application.dto.RegistrationDto;
import com.vetsoftware.app.registration.application.port.in.RegisterUserUseCase;
import com.vetsoftware.app.registration.application.port.in.VerifyEmailUseCase;
import com.vetsoftware.app.registration.infrastructure.web.request.RegisterUserRequest;
import com.vetsoftware.app.registration.infrastructure.web.request.VerifyEmailRequest;
import com.vetsoftware.app.registration.infrastructure.web.response.RegistrationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register")
public class RegistrationController {

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final AuditLogger auditLogger;

    public RegistrationController(RegisterUserUseCase registerUserUseCase,
            VerifyEmailUseCase verifyEmailUseCase, AuditLogger auditLogger) {
        this.registerUserUseCase = registerUserUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.auditLogger = auditLogger;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegisterUserRequest request,
            HttpServletRequest httpRequest) {
        RegistrationDto dto = registerUserUseCase.execute(new RegisterUserCommand(
                request.companyName(), request.documentType(), request.companyIdentifier(),
                request.companyAddress(), request.companyContactNumber(), request.cityId(),
                request.employeeName(), request.employeeEmail(), request.password(),
                request.taxRegime(), request.fiscalEmail(), request.recaptchaToken(),
                httpRequest.getRemoteAddr()));
        // Auditoría: alta de nueva veterinaria (empresa) por auto-registro. El usuario
        // de acceso es el
        // correo.
        auditLogger.companyRegistered(dto.companyId(), request.companyName(),
                request.companyIdentifier(), dto.employeeId(), dto.email());
        return new RegistrationResponse(dto.companyId(), dto.employeeId(), dto.email(),
                dto.status());
    }

    /**
     * Confirma el correo del dueño con el token recibido por email (Opción B). Tras
     * esto, ya puede iniciar sesión.
     */
    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmailUseCase.execute(new VerifyEmailCommand(request.token()));
    }
}
