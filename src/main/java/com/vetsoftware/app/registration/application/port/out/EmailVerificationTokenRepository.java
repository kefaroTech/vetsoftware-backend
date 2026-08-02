package com.vetsoftware.app.registration.application.port.out;

import com.vetsoftware.app.registration.domain.EmailVerificationToken;
import java.util.Optional;

public interface EmailVerificationTokenRepository {
  EmailVerificationToken save(EmailVerificationToken token);

  Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
