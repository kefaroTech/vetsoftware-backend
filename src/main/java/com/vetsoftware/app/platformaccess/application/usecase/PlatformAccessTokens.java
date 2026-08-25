package com.vetsoftware.app.platformaccess.application.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generacion y hashing de los secretos del flujo. Calca el patron de
 * {@code PasswordResetTokens}: el valor plano viaja en el correo, en la base
 * solo queda su hash, y al validar se re-hashea lo recibido y se busca por
 * hash, de modo que un volcado de la tabla no entrega tokens usables.
 *
 * <p>
 * <b>Los dos secretos NO se tratan igual, y esa asimetria es el nucleo del
 * diseno.</b>
 *
 * <ul>
 * <li>El <b>token</b> son 32 bytes de {@link SecureRandom}: 256 bits, muy por
 * encima de los 128 exigidos. SHA-256 sin salt es correcto para el —no hay
 * diccionario que atacar sobre un aleatorio de 256 bits— y el hash solo esta
 * para que la tabla no entregue credenciales.</li>
 * <li>El <b>codigo</b> son 6 digitos: 10^6 combinaciones, unos 20 bits. Con
 * SHA-256 seria practicamente guardarlo en claro. Va con bcrypt, y por eso su
 * hashing NO vive aqui sino detras de {@code SecretHasherPort}.</li>
 * </ul>
 */
final class PlatformAccessTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 10^6: seis digitos, con ceros a la izquierda cuando toque. */
    private static final int VERIFICATION_CODE_BOUND = 1_000_000;

    private PlatformAccessTokens() {
    }

    /** 32 bytes aleatorios en Base64 URL-safe sin padding (unos 43 caracteres). */
    static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Codigo de verificacion de exactamente 6 digitos, con {@link SecureRandom} y
     * no con {@code Math.random()}: es la mitad de la credencial que aprueba la
     * creacion de un superadministrador. Se rellena con ceros a la izquierda para
     * que el espacio sea el millon completo y no 900.000.
     */
    static String generateVerificationCode() {
        return String.format("%06d", RANDOM.nextInt(VERIFICATION_CODE_BOUND));
    }

    /**
     * SHA-256 del token plano en hex (64 caracteres): la columna es VARCHAR(64).
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
