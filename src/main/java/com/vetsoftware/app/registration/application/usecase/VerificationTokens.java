package com.vetsoftware.app.registration.application.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generacion y hashing de tokens de verificacion de correo. El valor plano (raw) viaja en el email;
 * en BD solo se guarda su hash SHA-256. Al verificar se re-hashea el valor recibido y se busca por hash,
 * de modo que una filtracion de la tabla no revela tokens usables.
 */
final class VerificationTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private VerificationTokens() {}

    /** 32 bytes aleatorios en Base64 URL-safe sin padding (~43 chars). */
    static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 del token plano, en hex (64 chars) — coincide con la columna token_hash VARCHAR(64). */
    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
