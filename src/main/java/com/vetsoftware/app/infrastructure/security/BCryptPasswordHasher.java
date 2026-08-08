package com.vetsoftware.app.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    /**
     * BE-07: el constructor sin argumentos usa cost 10, por debajo del mínimo de 12
     * que OWASP recomienda para 2026.
     *
     * <p>
     * Subirlo no invalida nada de lo ya guardado: cada hash BCrypt lleva su propio
     * cost embebido ({@code $2a$10$…}) y {@code matches} lo lee de ahí, así que las
     * credenciales existentes siguen verificando. Solo las que se generen a partir
     * de ahora usan el cost nuevo.
     */
    private static final int COST = 12;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(COST);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        if (storedHash == null || storedHash.isBlank())
            return false;
        return encoder.matches(rawPassword, storedHash);
    }
}
