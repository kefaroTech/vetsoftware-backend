package com.vetsoftware.app.auth.infrastructure.web;

import com.vetsoftware.app.auth.application.command.LoginEmployeeCommand;
import com.vetsoftware.app.auth.application.command.LoginSystemUserCommand;
import com.vetsoftware.app.auth.application.dto.MeDto;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.auth.application.exception.InvalidCredentialsException;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
    private final RefreshTokenCookie refreshTokenCookie;

    public AuthController(LoginEmployeeUseCase loginEmployeeUseCase,
            LoginSystemUserUseCase loginSystemUserUseCase,
            GetCurrentUserUseCase getCurrentUserUseCase, RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase, AuditLogger auditLogger,
            RefreshTokenCookie refreshTokenCookie) {
        this.loginEmployeeUseCase = loginEmployeeUseCase;
        this.loginSystemUserUseCase = loginSystemUserUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.auditLogger = auditLogger;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    /**
     * El {@code employeeCode} que se audita <b>puede ser un correo</b>: en el
     * auto-registro el código de acceso del dueño se deriva de su email
     * ({@code RegisterUserService.register}). No se redacta aquí a propósito —
     * {@code actor.identifier} está declarado {@code SCANNED} en
     * {@code LogFieldPolicy} (#216) y el pipeline lo enmascara a
     * {@code ***@dominio} antes de salir del proceso, para todo emisor y no solo
     * para el que se acordó.
     */
    @PostMapping("/login/employee")
    public ResponseEntity<TokenResponse> loginEmployee(
            @Valid @RequestBody LoginEmployeeRequest request) {
        TokenDto dto = loginEmployeeUseCase
                .execute(new LoginEmployeeCommand(request.employeeCode(), request.password()));
        auditLogger.loginSuccess("EMPLOYEE", request.employeeCode());
        return withRefreshCookie(dto);
    }

    @PostMapping("/login/system")
    public ResponseEntity<TokenResponse> loginSystemUser(
            @Valid @RequestBody LoginSystemUserRequest request) {
        TokenDto dto = loginSystemUserUseCase
                .execute(new LoginSystemUserCommand(request.code(), request.password()));
        auditLogger.loginSuccess("SYSTEM_USER", request.code());
        return withRefreshCookie(dto);
    }

    /**
     * El refresh token llega en la cookie {@code HttpOnly}, no en el cuerpo. El
     * cuerpo se sigue aceptando como respaldo durante el despliegue coordinado:
     * backend y los dos frontends no se publican en el mismo instante, y sin esta
     * tolerancia el front antiguo pierde la sesión en cuanto sube el backend nuevo.
     * Es transitorio y debe retirarse cuando los dos fronts estén arriba.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest request) {
        String raw = StringUtils.hasText(cookieToken)
                ? cookieToken
                : request != null ? request.refreshToken() : null;
        if (!StringUtils.hasText(raw)) {
            throw new InvalidCredentialsException();
        }
        return withRefreshCookie(refreshTokenUseCase.execute(raw));
    }

    /**
     * Borra la cookie además de revocar en servidor. Si solo se revocara, el
     * navegador seguiría enviando un token muerto en cada {@code /auth/refresh} y
     * el usuario vería un 401 en vez de la pantalla de login.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        logoutUseCase.execute();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString()).build();
    }

    /**
     * El {@code refreshToken} viaja en la cabecera {@code Set-Cookie} y sale del
     * cuerpo. Que el campo del JSON quede en {@code null} es deliberado: mantiene
     * la forma de la respuesta y hace evidente en un log o en el inspector que el
     * valor ya no se entrega por ahí.
     */
    private ResponseEntity<TokenResponse> withRefreshCookie(TokenDto dto) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshTokenCookie.issue(dto.refreshToken()).toString())
                .body(new TokenResponse(dto.token(), dto.type(), null));
    }

    @GetMapping("/me")
    public MeResponse me() {
        MeDto dto = getCurrentUserUseCase.execute();
        return new MeResponse(dto.id(), dto.type(), dto.companyId(), dto.name(), dto.employeeCode(),
                dto.mustChangePassword(), new ArrayList<>(dto.permissions()),
                new ArrayList<>(dto.branchIds()));
    }
}
