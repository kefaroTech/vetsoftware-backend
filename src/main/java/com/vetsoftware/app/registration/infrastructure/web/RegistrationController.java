package com.vetsoftware.app.registration.infrastructure.web;

import com.vetsoftware.app.registration.application.command.RegisterUserCommand;
import com.vetsoftware.app.registration.application.dto.RegistrationDto;
import com.vetsoftware.app.registration.application.port.in.RegisterUserUseCase;
import com.vetsoftware.app.registration.infrastructure.web.request.RegisterUserRequest;
import com.vetsoftware.app.registration.infrastructure.web.response.RegistrationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register")
public class RegistrationController {

    private final RegisterUserUseCase registerUserUseCase;

    public RegistrationController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegisterUserRequest request) {
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
            request.fiscalEmail()
        ));
        return new RegistrationResponse(dto.companyId(), dto.employeeId(), dto.token(), dto.tokenType());
    }
}
