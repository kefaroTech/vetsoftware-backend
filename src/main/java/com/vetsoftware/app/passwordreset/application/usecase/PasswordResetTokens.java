package com.vetsoftware.app.passwordreset.application.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generación y hashing de tokens de restablecimiento. El valor plano (raw)
 * viaja en el enlace del correo; en BD solo se guarda su hash SHA-256. Al
 * validar/usar se re-hashea el valor recibido y se busca por hash, de modo que
 * una filtración de la tabla no revela tokens usables.
 */
final class PasswordResetTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordResetTokens() {
    }

    /** 32 bytes aleatorios en Base64 URL-safe sin padding (~43 chars). */
    static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 del token plano, en hex (64 chars) — coincide con la columna
     * token_hash VARCHAR(64).
     */
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
