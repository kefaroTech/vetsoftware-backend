package com.vetsoftware.app.passwordreset.infrastructure.web;

import com.vetsoftware.app.passwordreset.application.command.RequestPasswordResetCommand;
import com.vetsoftware.app.passwordreset.application.command.ResetPasswordCommand;
import com.vetsoftware.app.passwordreset.application.port.in.RequestPasswordResetUseCase;
import com.vetsoftware.app.passwordreset.application.port.in.ResetPasswordUseCase;
import com.vetsoftware.app.passwordreset.application.port.in.ValidatePasswordResetTokenUseCase;
import com.vetsoftware.app.passwordreset.infrastructure.web.request.ForgotPasswordRequest;
import com.vetsoftware.app.passwordreset.infrastructure.web.request.ResetPasswordRequest;
import com.vetsoftware.app.passwordreset.infrastructure.web.response.ValidateResetTokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    private final RequestPasswordResetUseCase requestUseCase;
    private final ValidatePasswordResetTokenUseCase validateUseCase;
    private final ResetPasswordUseCase resetUseCase;

    public PasswordResetController(RequestPasswordResetUseCase requestUseCase,
            ValidatePasswordResetTokenUseCase validateUseCase, ResetPasswordUseCase resetUseCase) {
        this.requestUseCase = requestUseCase;
        this.validateUseCase = validateUseCase;
        this.resetUseCase = resetUseCase;
    }

    /**
     * Solicita el restablecimiento por código. Responde 204 SIEMPRE
     * (anti-enumeración: no revela si existe).
     */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        requestUseCase.execute(new RequestPasswordResetCommand(request.employeeCode()));
    }

    /**
     * ¿El token del enlace sigue siendo usable? (la pantalla de reset lo consulta
     * al montar).
     */
    @GetMapping("/reset-password/validate")
    public ValidateResetTokenResponse validate(@RequestParam String token) {
        return new ValidateResetTokenResponse(validateUseCase.isValid(token));
    }

    /**
     * Confirma el restablecimiento: consume el token y aplica la nueva contraseña.
     */
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        resetUseCase.execute(new ResetPasswordCommand(request.token(), request.newPassword()));
    }
}
