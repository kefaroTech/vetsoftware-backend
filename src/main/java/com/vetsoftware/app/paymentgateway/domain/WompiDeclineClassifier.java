package com.vetsoftware.app.paymentgateway.domain;

import java.util.Locale;

/**
 * Clasifica el {@code status_message} crudo de una transacción rechazada.
 *
 * <p>
 * Wompi no tiene un catálogo cerrado de motivos: es texto libre del emisor.
 * Esta clasificación es <strong>heurística y conservadora</strong> —todo lo que
 * no reconoce cae en {@link GatewayDeclineKind#SOFT}, nunca en {@code HARD}—,
 * porque declarar {@code HARD} por error le cierra al cliente la puerta de
 * reintentar con la misma tarjeta.
 */
public final class WompiDeclineClassifier {

    private WompiDeclineClassifier() {
    }

    public static GatewayDeclineKind classify(GatewayTransactionStatus status,
            String statusMessage) {
        if (status == GatewayTransactionStatus.ERROR)
            return GatewayDeclineKind.CONFIGURATION;
        if (statusMessage == null || statusMessage.isBlank())
            return GatewayDeclineKind.SOFT;
        String normalized = statusMessage.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "fondos", "insufficient", "limit"))
            return GatewayDeclineKind.SOFT;
        if (containsAny(normalized, "robada", "stolen", "perdida", "lost", "restringida",
                "restricted", "invalid", "expirada", "expired"))
            return GatewayDeclineKind.HARD;
        return GatewayDeclineKind.SOFT;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle))
                return true;
        }
        return false;
    }
}
