package com.vetsoftware.app.passwordreset.application.usecase;

import com.vetsoftware.app.passwordreset.application.command.ResetPasswordCommand;
import com.vetsoftware.app.passwordreset.application.port.in.ResetPasswordUseCase;
import com.vetsoftware.app.passwordreset.application.port.out.EmployeePasswordResetter;
import com.vetsoftware.app.passwordreset.application.port.out.PasswordResetTokenRepository;
import com.vetsoftware.app.passwordreset.domain.InvalidPasswordResetTokenException;
import com.vetsoftware.app.passwordreset.domain.PasswordResetToken;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirma el restablecimiento: re-hashea el token plano, lo consume de forma irreversible y aplica la nueva
 * contraseña al empleado (que además invalida sus sesiones vivas). La posesión del token es la autorización.
 */
@Service
public class ResetPasswordService implements ResetPasswordUseCase {

    private final PasswordResetTokenRepository tokenRepository;
    private final EmployeePasswordResetter employeePasswordResetter;

    public ResetPasswordService(PasswordResetTokenRepository tokenRepository,
                                EmployeePasswordResetter employeePasswordResetter) {
        this.tokenRepository = tokenRepository;
        this.employeePasswordResetter = employeePasswordResetter;
    }

    @Override
    @Transactional
    public void execute(ResetPasswordCommand command) {
        if (command.token() == null || command.token().isBlank()) {
            throw new InvalidPasswordResetTokenException("Password reset token is required");
        }
        String hash = PasswordResetTokens.hash(command.token());
        PasswordResetToken token = tokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid password reset token"));

        token.consume(LocalDateTime.now());
        tokenRepository.save(token);

        employeePasswordResetter.reset(token.getEmployeeId(), command.newPassword());
    }
}
