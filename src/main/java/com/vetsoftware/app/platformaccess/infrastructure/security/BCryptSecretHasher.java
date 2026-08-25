package com.vetsoftware.app.platformaccess.infrastructure.security;

import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import org.springframework.stereotype.Component;

/**
 * Adaptador del hashing de secretos de esta feature: delega en el mismo bcrypt
 * que usa el resto del sistema.
 *
 * <p>
 * Existe para que la capa de aplicacion no importe {@code infrastructure}. No
 * anade logica: si alguna vez hiciera falta un coste distinto para el codigo de
 * 6 digitos que para la contrasena, este es el sitio donde se veria.
 */
@Component
public class BCryptSecretHasher implements SecretHasherPort {

    private final PasswordHasher passwordHasher;

    public BCryptSecretHasher(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
    }

    @Override
    public String hash(String rawSecret) {
        return passwordHasher.hash(rawSecret);
    }

    @Override
    public boolean matches(String rawSecret, String storedHash) {
        return passwordHasher.matches(rawSecret, storedHash);
    }
}
