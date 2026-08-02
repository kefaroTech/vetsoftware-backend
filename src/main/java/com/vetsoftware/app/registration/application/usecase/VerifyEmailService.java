package com.vetsoftware.app.registration.application.usecase;

import com.vetsoftware.app.registration.application.command.VerifyEmailCommand;
import com.vetsoftware.app.registration.application.port.in.VerifyEmailUseCase;
import com.vetsoftware.app.registration.application.port.out.EmailVerificationTokenRepository;
import com.vetsoftware.app.registration.application.port.out.EmployeeEmailVerifier;
import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import com.vetsoftware.app.registration.domain.InvalidVerificationTokenException;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verificacion de correo (Opcion B). Recibe el token plano, lo re-hashea, lo consume de forma
 * irreversible y marca al empleado como verificado, habilitando su login. La posesion del token es
 * la autorizacion.
 */
@Observed(name = "registration.verify.email")
@Service
public class VerifyEmailService implements VerifyEmailUseCase {

  private final EmailVerificationTokenRepository tokenRepository;
  private final EmployeeEmailVerifier employeeEmailVerifier;

  public VerifyEmailService(
      EmailVerificationTokenRepository tokenRepository,
      EmployeeEmailVerifier employeeEmailVerifier) {
    this.tokenRepository = tokenRepository;
    this.employeeEmailVerifier = employeeEmailVerifier;
  }

  @Override
  @Transactional
  public void execute(VerifyEmailCommand command) {
    if (command.token() == null || command.token().isBlank()) {
      throw new InvalidVerificationTokenException("Verification token is required");
    }
    String hash = VerificationTokens.hash(command.token());
    EmailVerificationToken token =
        tokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> new InvalidVerificationTokenException("Invalid verification token"));

    token.consume(LocalDateTime.now());
    tokenRepository.save(token);

    employeeEmailVerifier.verify(token.getEmployeeId());
  }
}
