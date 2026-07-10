package com.vetsoftware.app.auth.infrastructure.web;

import com.vetsoftware.app.auth.application.command.LoginEmployeeCommand;
import com.vetsoftware.app.auth.application.command.LoginSystemUserCommand;
import com.vetsoftware.app.auth.application.dto.MeDto;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.port.in.GetCurrentUserUseCase;
import com.vetsoftware.app.auth.application.port.in.LoginEmployeeUseCase;
import com.vetsoftware.app.auth.application.port.in.LoginSystemUserUseCase;
import com.vetsoftware.app.auth.application.port.in.LogoutUseCase;
import com.vetsoftware.app.auth.application.port.in.RefreshTokenUseCase;
import com.vetsoftware.app.auth.infrastructure.web.request.LoginEmployeeRequest;
import com.vetsoftware.app.auth.infrastructure.web.request.LoginSystemUserRequest;
import com.vetsoftware.app.auth.infrastructure.web.request.RefreshTokenRequest;
import com.vetsoftware.app.auth.infrastructure.web.response.MeResponse;
import com.vetsoftware.app.auth.infrastructure.web.response.TokenResponse;
import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import jakarta.validation.Valid;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginEmployeeUseCase loginEmployeeUseCase;
    private final LoginSystemUserUseCase loginSystemUserUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuditLogger auditLogger;

    public AuthController(LoginEmployeeUseCase loginEmployeeUseCase,
                          LoginSystemUserUseCase loginSystemUserUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          LogoutUseCase logoutUseCase,
                          AuditLogger auditLogger) {
        this.loginEmployeeUseCase = loginEmployeeUseCase;
        this.loginSystemUserUseCase = loginSystemUserUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/login/employee")
    public TokenResponse loginEmployee(@Valid @RequestBody LoginEmployeeRequest request) {
        TokenDto dto = loginEmployeeUseCase.execute(
                new LoginEmployeeCommand(request.employeeCode(), request.password())
        );
        auditLogger.loginSuccess("EMPLOYEE", request.employeeCode());
        return new TokenResponse(dto.token(), dto.type(), dto.refreshToken());
    }

    @PostMapping("/login/system")
    public TokenResponse loginSystemUser(@Valid @RequestBody LoginSystemUserRequest request) {
        TokenDto dto = loginSystemUserUseCase.execute(
                new LoginSystemUserCommand(request.code(), request.password())
        );
        auditLogger.loginSuccess("SYSTEM_USER", request.code());
        return new TokenResponse(dto.token(), dto.type(), dto.refreshToken());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenDto dto = refreshTokenUseCase.execute(request.refreshToken());
        return new TokenResponse(dto.token(), dto.type(), dto.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        logoutUseCase.execute();
    }

    @GetMapping("/me")
    public MeResponse me() {
        MeDto dto = getCurrentUserUseCase.execute();
        return new MeResponse(
                dto.id(),
                dto.type(),
                dto.companyId(),
                dto.name(),
                dto.employeeCode(),
                dto.mustChangePassword(),
                new ArrayList<>(dto.permissions())
        );
    }
}
