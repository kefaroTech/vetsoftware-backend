package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Las dos firmas SHA-256 de Wompi.
 *
 * <p>
 * <strong>Sin caché ni estado</strong>: ambas son funciones puras sobre los
 * valores que ya trae la petición o la respuesta. La comparación del checksum
 * es en tiempo constante ({@link MessageDigest#isEqual}) porque compara un
 * secreto de plataforma contra un valor que manda el mundo exterior.
 */
public final class WompiSignatures {

    private WompiSignatures() {
    }

    /**
     * Firma de integridad de {@code POST /transactions}:
     * {@code reference + amountInCents + currency + integritySecret}, en ese orden
     * y sin separadores.
     */
    public static String integritySignature(String reference, long amountInCents, String currency,
            String integritySecret) {
        return sha256Hex(reference + amountInCents + currency + integritySecret);
    }

    /**
     * Checksum de un evento: los valores de {@code signature.properties} (en el
     * orden en que Wompi los declaró) más {@code timestamp} más el secreto de
     * eventos, sin separadores.
     */
    public static String eventChecksum(List<String> propertyValues, long timestamp,
            String eventsSecret) {
        StringBuilder raw = new StringBuilder();
        for (String value : propertyValues) {
            raw.append(value);
        }
        raw.append(timestamp).append(eventsSecret);
        return sha256Hex(raw.toString());
    }

    /** {@code true} si {@code checksumHeader} coincide con el checksum esperado. */
    public static boolean matchesEventChecksum(String checksumHeader, List<String> propertyValues,
            long timestamp, String eventsSecret) {
        if (checksumHeader == null)
            return false;
        String expected = eventChecksum(propertyValues, timestamp, eventsSecret);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                checksumHeader.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es un algoritmo obligatorio de toda JVM conforme (JLS/JCA): esta
            // rama es inalcanzable en un despliegue real.
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
