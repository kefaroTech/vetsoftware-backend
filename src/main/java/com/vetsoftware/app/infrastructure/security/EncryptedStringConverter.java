package com.vetsoftware.app.infrastructure.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cifra/descifra columnas de texto con AES-GCM. La clave (32 bytes en base64) se lee de la variable
 * de entorno {@code DIAN_ENC_KEY}; hay un default que SOLO se acepta en los perfiles dev/test/local.
 * Fuera de esos perfiles, si {@code DIAN_ENC_KEY} falta o esta vacia el arranque falla (fail-fast),
 * para no cifrar los secretos del proveedor DIAN con una clave publica del repo. El formato persistido
 * es base64(iv[12] || ciphertext+tag). Pensado para secretos reversibles (credenciales/tokens del
 * proveedor DIAN); NO para contrasenas de usuario (esas usan BCrypt, no reversible).
 *
 * Hibernate instancia este converter (no es un bean de Spring), por eso la clave se resuelve de forma
 * estatica desde el entorno.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedStringConverter.class);

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    // Default SOLO para desarrollo (32 bytes base64). En produccion DEBE sobreescribirse con DIAN_ENC_KEY.
    private static final String DEV_KEY_BASE64 = "REMOVED_SENSITIVE_VALUE";

    private static final SecretKeySpec KEY = loadKey();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static SecretKeySpec loadKey() {
        String configured = System.getenv("DIAN_ENC_KEY");
        boolean missing = (configured == null || configured.isBlank());
        if (missing) {
            if (!isDevOrTestProfile()) {
                throw new IllegalStateException(
                        "DIAN_ENC_KEY no esta definida y el perfil activo no es dev/test/local. "
                        + "Defina DIAN_ENC_KEY (32 bytes en base64) para cifrar los secretos del proveedor DIAN.");
            }
            log.warn("DIAN_ENC_KEY no definida: usando la clave de desarrollo embebida del repo. "
                    + "Solo aceptable en dev/test/local; NUNCA usar en produccion.");
        }
        byte[] key = Base64.getDecoder().decode(missing ? DEV_KEY_BASE64 : configured);
        if (key.length != 32) {
            throw new IllegalStateException("DIAN_ENC_KEY must decode to 32 bytes (AES-256)");
        }
        return new SecretKeySpec(key, "AES");
    }

    // El converter se inicializa de forma estatica (antes de que Spring resuelva el Environment),
    // por eso el perfil se lee directo de la system property / env var, con el mismo default (dev)
    // que application.yml (spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}).
    private static boolean isDevOrTestProfile() {
        String profiles = System.getProperty("spring.profiles.active");
        if (profiles == null || profiles.isBlank()) {
            profiles = System.getenv("SPRING_PROFILES_ACTIVE");
        }
        if (profiles == null || profiles.isBlank()) {
            profiles = "dev";
        }
        for (String profile : profiles.split(",")) {
            String normalized = profile.trim().toLowerCase();
            if (normalized.equals("dev") || normalized.equals("test") || normalized.equals("local")) {
                return true;
            }
        }
        return false;
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
