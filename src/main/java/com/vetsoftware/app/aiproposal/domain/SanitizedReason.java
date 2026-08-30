package com.vetsoftware.app.aiproposal.domain;

/**
 * El motivo tal como se puede persistir y servir, con la regla que lo toco.
 *
 * @param text
 *            el texto final: el del modelo, el del modelo truncado, o el
 *            {@code short_description} determinista
 * @param rule
 *            la regla que disparo, o {@code null} si el motivo salio intacto.
 *            Es la etiqueta de la metrica, no una cadena libre
 * @param substituted
 *            {@code true} si {@link #text()} ya no es prosa del modelo
 */
public record SanitizedReason(String text, ReasonRejection rule, boolean substituted) {

    public static SanitizedReason intacto(String text) {
        return new SanitizedReason(text, null, false);
    }

    public static SanitizedReason truncado(String text) {
        return new SanitizedReason(text, ReasonRejection.R2_LARGO, false);
    }

    public static SanitizedReason sustituido(String fallback, ReasonRejection rule) {
        return new SanitizedReason(fallback, rule, true);
    }

    /** {@code true} si hay algo que contar en la metrica. */
    public boolean hayQueRegistrar() {
        return rule != null;
    }
}
