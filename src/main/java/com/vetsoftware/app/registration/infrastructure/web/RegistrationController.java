package com.vetsoftware.app.registration.infrastructure.web;

import com.vetsoftware.app.registration.application.command.RegisterUserCommand;
import com.vetsoftware.app.registration.application.command.VerifyEmailCommand;
import com.vetsoftware.app.registration.application.dto.RegistrationDto;
import com.vetsoftware.app.registration.application.port.in.CheckEmployeeCodeAvailabilityUseCase;
import com.vetsoftware.app.registration.application.port.in.RegisterUserUseCase;
import com.vetsoftware.app.registration.application.port.in.SuggestEmployeeCodeUseCase;
import com.vetsoftware.app.registration.application.port.in.VerifyEmailUseCase;
import com.vetsoftware.app.registration.infrastructure.web.request.RegisterUserRequest;
import com.vetsoftware.app.registration.infrastructure.web.request.VerifyEmailRequest;
import com.vetsoftware.app.registration.infrastructure.web.response.EmployeeCodeAvailabilityResponse;
import com.vetsoftware.app.registration.infrastructure.web.response.EmployeeCodeSuggestionResponse;
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
    private final SuggestEmployeeCodeUseCase suggestEmployeeCodeUseCase;
    private final CheckEmployeeCodeAvailabilityUseCase checkEmployeeCodeAvailabilityUseCase;

    public RegistrationController(RegisterUserUseCase registerUserUseCase,
                                  VerifyEmailUseCase verifyEmailUseCase,
                                  SuggestEmployeeCodeUseCase suggestEmployeeCodeUseCase,
                                  CheckEmployeeCodeAvailabilityUseCase checkEmployeeCodeAvailabilityUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.suggestEmployeeCodeUseCase = suggestEmployeeCodeUseCase;
        this.checkEmployeeCodeAvailabilityUseCase = checkEmployeeCodeAvailabilityUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegisterUserRequest request,
                                         HttpServletRequest httpRequest) {
        RegistrationDto dto = registerUserUseCase.execute(new RegisterUserCommand(
            request.companyName(),
            request.documentType(),
            request.companyIdentifier(),
            request.companyAddress(),
            request.companyContactNumber(),
            request.cityId(),
            request.employeeName(),
            request.employeeEmail(),
            request.password(),
            request.taxRegime(),
            request.fiscalEmail(),
            request.recaptchaToken(),
            httpRequest.getRemoteAddr(),
            request.employeeCode()
        ));
        return new RegistrationResponse(
            dto.companyId(), dto.employeeId(), dto.email(), dto.employeeCode(), dto.status());
    }

    /** Confirma el correo del dueño con el token recibido por email (Opción B). Tras esto, ya puede iniciar sesión. */
    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmailUseCase.execute(new VerifyEmailCommand(request.token()));
    }

    /** Sugiere un usuario de acceso disponible a partir de la empresa y el nombre (Opción A). */
    @GetMapping("/suggest-code")
    public EmployeeCodeSuggestionResponse suggestCode(
            @RequestParam(required = false, defaultValue = "") String companyName,
            @RequestParam(required = false, defaultValue = "") String employeeName) {
        return new EmployeeCodeSuggestionResponse(
            suggestEmployeeCodeUseCase.suggest(companyName, employeeName));
    }

    /** Indica si un usuario de acceso está libre (chequeo en vivo del formulario). */
    @GetMapping("/code-availability")
    public EmployeeCodeAvailabilityResponse codeAvailability(@RequestParam String code) {
        return new EmployeeCodeAvailabilityResponse(
            checkEmployeeCodeAvailabilityUseCase.isAvailable(code));
    }
}
