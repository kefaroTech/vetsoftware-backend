package com.vetsoftware.app.infrastructure.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra/descifra columnas de texto con AES-GCM. La clave (32 bytes en base64) se lee de la variable
 * de entorno {@code DIAN_ENC_KEY}; hay un default solo para desarrollo. El formato persistido es
 * base64(iv[12] || ciphertext+tag). Pensado para secretos reversibles (credenciales/tokens del
 * proveedor DIAN); NO para contrasenas de usuario (esas usan BCrypt, no reversible).
 *
 * Hibernate instancia este converter (no es un bean de Spring), por eso la clave se resuelve de forma
 * estatica desde el entorno.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    // Default SOLO para desarrollo (32 bytes base64). En produccion DEBE sobreescribirse con DIAN_ENC_KEY.
    private static final String DEV_KEY_BASE64 = "REMOVED_SENSITIVE_VALUE";

    private static final SecretKeySpec KEY = loadKey();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static SecretKeySpec loadKey() {
        String configured = System.getenv("DIAN_ENC_KEY");
        byte[] key = Base64.getDecoder().decode(
                (configured == null || configured.isBlank()) ? DEV_KEY_BASE64 : configured);
        if (key.length != 32) {
            throw new IllegalStateException("DIAN_ENC_KEY must decode to 32 bytes (AES-256)");
        }
        return new SecretKeySpec(key, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, KEY, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt value", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] in = Base64.getDecoder().decode(dbData);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(in, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[in.length - IV_LENGTH];
            System.arraycopy(in, IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt value", e);
        }
    }
}
