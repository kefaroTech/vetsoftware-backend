package com.vetsoftware.app.infrastructure.audit.chain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Primitivas de la cadena de hash de auditoría.
 *
 * <p>El eslabón de la posición {@code n} es:
 *
 * <pre>chain_hash(n) = SHA-256( previous_hash(n) + ":" + n + ":" + payload_hash(n) )</pre>
 *
 * <p>donde {@code previous_hash(n)} es {@code chain_hash(n-1)} y el eslabón cero usa
 * {@link #GENESIS_HASH}. Encadenar así hace que suprimir, reordenar o alterar cualquier evento
 * invalide todos los eslabones posteriores, de modo que la manipulación es detectable aunque el
 * atacante tenga permisos de escritura sobre la tabla.
 *
 * <p>Todos los hexadecimales son minúsculos para coincidir con {@code SHA2(x, 256)} de MySQL, que
 * es lo que usa el relleno de la migración 215.
 */
public final class AuditChainHash {

    /** {@code previous_hash} del primer eslabón. 64 ceros, no es un hash real de nada. */
    public static final String GENESIS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private static final HexFormat HEX = HexFormat.of().withLowerCase();

    private AuditChainHash() {
    }

    /**
     * Hash del payload tal como se serializó. Se calcula sobre los bytes UTF-8 exactos, por lo que
     * la columna debe conservarlos sin normalizar (ver migración 215: {@code LONGTEXT}, no
     * {@code JSON}).
     */
    public static String payloadHash(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload es obligatorio para calcular su hash");
        }
        return sha256Hex(payload);
    }

    /** Eslabón de la cadena. Ver la fórmula en la documentación de la clase. */
    public static String chainHash(String previousHash, long sequence, String payloadHash) {
        requireHex(previousHash, "previousHash");
        requireHex(payloadHash, "payloadHash");
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence debe ser positiva: " + sequence);
        }
        return sha256Hex(previousHash + ":" + sequence + ":" + payloadHash);
    }

    private static String sha256Hex(String value) {
        return HEX.formatHex(digest().digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 es obligatorio en toda implementación de la plataforma Java.
            throw new IllegalStateException("SHA-256 no disponible", exception);
        }
    }

    private static void requireHex(String value, String name) {
        if (value == null || value.length() != 64) {
            throw new IllegalArgumentException(name + " debe ser un SHA-256 hex de 64 caracteres");
        }
        for (int index = 0; index < 64; index++) {
            char character = value.charAt(index);
            boolean valid = (character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f');
            if (!valid) {
                throw new IllegalArgumentException(name + " debe ser hexadecimal minúsculo");
            }
        }
    }
}
