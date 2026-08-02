package com.vetsoftware.app.infrastructure.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra/descifra columnas de texto con AES-GCM. La clave (32 bytes en base64) se lee de la variable
 * de entorno {@code DIAN_ENC_KEY}. No existe una clave predeterminada en el código: todos los perfiles,
 * incluidos local y test, deben inyectar una clave propia de 32 bytes en base64. Si falta, el arranque
 * falla inmediatamente para impedir que datos sensibles se cifren con material público o compartido.
 *
 * <p>Rotación: el formato persistido lleva un prefijo de versión de clave ({@code "<version>:base64(iv||ct)}").
 * El cifrado usa la clave y versión activas; el descifrado selecciona la clave por versión, admitiendo una
 * clave anterior opcional ({@code DIAN_ENC_KEY_PREVIOUS} + {@code DIAN_ENC_KEY_PREVIOUS_VERSION}) mientras se
 * re-cifran los datos. Los valores en formato legado (sin prefijo, escritos antes de esta versión) se
 * descifran con la clave activa. El separador {@code ':'} nunca aparece en base64, así que el prefijo es
 * inequívoco. Pensado para secretos reversibles (credenciales/tokens del proveedor DIAN); NO para
 * contraseñas de usuario (esas usan BCrypt, no reversible).
 *
 * <p>Hibernate instancia este converter (no es un bean de Spring), por eso la clave se resuelve de forma
 * estática desde el entorno.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    // Version por defecto de la clave productiva si no se define DIAN_ENC_KEY_VERSION.
    private static final String DEFAULT_KEY_VERSION = "v1";
    // Separa "<version>:<payload base64>" en el formato persistido. El alfabeto base64 no incluye ':'.
    private static final char VERSION_SEPARATOR = ':';

    private static final KeyMaterial MATERIAL = loadKeys();
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Clave activa (para cifrar) + versiones conocidas (para descifrar, incluida la anterior en rotación). */
    private static final class KeyMaterial {
        final String activeVersion;
        final SecretKeySpec activeKey;
        final Map<String, SecretKeySpec> keysByVersion;

        KeyMaterial(String activeVersion, Map<String, SecretKeySpec> keysByVersion) {
            this.activeVersion = activeVersion;
            this.activeKey = keysByVersion.get(activeVersion);
            this.keysByVersion = Map.copyOf(keysByVersion);
        }
    }

    private static KeyMaterial loadKeys() {
        Map<String, SecretKeySpec> keys = new HashMap<>();
        String configured = System.getenv("DIAN_ENC_KEY");
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException(
                    "DIAN_ENC_KEY no está definida. Genere una clave AES-256 propia (32 bytes en base64) "
                    + "fuera de Git e inyéctela mediante el entorno en todos los perfiles.");
        }
        String activeVersion = resolveVersion(System.getenv("DIAN_ENC_KEY_VERSION"), DEFAULT_KEY_VERSION);
        keys.put(activeVersion, toKey(configured));
        // Clave anterior opcional: permite descifrar los valores aún cifrados con ella durante una rotación.
        String previous = System.getenv("DIAN_ENC_KEY_PREVIOUS");
        if (previous != null && !previous.isBlank()) {
            String previousVersion = System.getenv("DIAN_ENC_KEY_PREVIOUS_VERSION");
            if (previousVersion == null || previousVersion.isBlank()) {
                throw new IllegalStateException(
                        "DIAN_ENC_KEY_PREVIOUS definida sin DIAN_ENC_KEY_PREVIOUS_VERSION: indique la versión "
                        + "de la clave anterior para poder descifrar los valores cifrados con ella.");
            }
            String normalized = previousVersion.trim();
            if (normalized.equals(activeVersion)) {
                throw new IllegalStateException(
                        "DIAN_ENC_KEY_PREVIOUS_VERSION no puede coincidir con DIAN_ENC_KEY_VERSION ("
                        + activeVersion + "): use una etiqueta de versión distinta para la clave anterior.");
            }
            keys.put(normalized, toKey(previous));
        }
        return new KeyMaterial(activeVersion, keys);
    }

    private static String resolveVersion(String configured, String fallback) {
        String version = (configured == null || configured.isBlank()) ? fallback : configured.trim();
        if (version.indexOf(VERSION_SEPARATOR) >= 0) {
            throw new IllegalStateException("la versión de clave no puede contener '" + VERSION_SEPARATOR + "'");
        }
        return version;
    }

    private static SecretKeySpec toKey(String base64) {
        byte[] key = Base64.getDecoder().decode(base64.trim());
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
            cipher.init(Cipher.ENCRYPT_MODE, MATERIAL.activeKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return MATERIAL.activeVersion + VERSION_SEPARATOR + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt value", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            // Selecciona la clave por el prefijo de versión. Formato legado (sin ':') → clave activa.
            SecretKeySpec key = MATERIAL.activeKey;
            String payload = dbData;
            int separator = dbData.indexOf(VERSION_SEPARATOR);
            if (separator >= 0) {
                String version = dbData.substring(0, separator);
                key = MATERIAL.keysByVersion.get(version);
                if (key == null) {
                    throw new IllegalStateException(
                            "No hay clave para la versión '" + version + "' del valor cifrado; "
                            + "configure DIAN_ENC_KEY_PREVIOUS con esa versión para descifrarlo.");
                }
                payload = dbData.substring(separator + 1);
            }
            byte[] in = Base64.getDecoder().decode(payload);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(in, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[in.length - IV_LENGTH];
            System.arraycopy(in, IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt value", e);
        }
    }
}
