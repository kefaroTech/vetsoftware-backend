package com.vetsoftware.app.aiproposal.domain;

/**
 * Que decidio el motor determinista sobre el codigo que propuso el modelo
 * ({@code chk_ai_proposal_lines_verdict}).
 *
 * <p>
 * ⛔ <strong>NINGUNO DE LOS CINCO VALORES SE SERIALIZA JAMAS POR HTTP</strong>
 * (plan S4.2.3). Son telemetria interna de calidad del modelo. El endpoint es
 * <em>driveable</em> -el texto de entrada lo escribe quien llama-, asi que
 * devolver el veredicto linea a linea convertiria la respuesta en un oraculo de
 * cinco valores sobre el catalogo interno: si el codigo existe, si existe pero
 * esta en borrador, si existe y esta retirado, o si existe y no se vende por
 * autoservicio. Es exactamente la fuga que {@code ARTICULO_NO_CONTRATABLE}
 * costo cerrar. Hacia fuera va, como mucho, un entero sin desglose y sin
 * causas: {@code CartResult.descartadas()}.
 */
public enum LineVerdict {

    ACCEPTED,

    UNKNOWN_CODE,

    NOT_SELLABLE,

    NOT_SELF_SERVICE,

    DUPLICATE;

    public boolean esAceptado() {
        return this == ACCEPTED;
    }
}
