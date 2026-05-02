package com.vetsoftware.app.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) return false;
        return encoder.matches(rawPassword, storedHash);
    }
}
