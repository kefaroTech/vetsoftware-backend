package com.vetsoftware.app.aiproposal.domain;

/**
 * Que decidio el motor determinista sobre el codigo que propuso el modelo
 * ({@code chk_ai_proposal_lines_verdict}).
 *
 * <p>
 * ⛔ <strong>NINGUNO DE LOS SEIS VALORES SE SERIALIZA JAMAS POR HTTP</strong>
 * (plan S4.2.3). Son telemetria interna de calidad del modelo. El endpoint es
 * <em>driveable</em> -el texto de entrada lo escribe quien llama-, asi que
 * devolver el veredicto linea a linea convertiria la respuesta en un oraculo de
 * seis valores sobre el catalogo interno: si el codigo existe, si existe pero
 * esta en borrador, si existe y esta retirado, si existe y no se vende por
 * autoservicio, o si es una capacidad que el sistema dimensiona solo. Es
 * exactamente la fuga que {@code ARTICULO_NO_CONTRATABLE} costo cerrar. Hacia
 * fuera va, como mucho, un entero sin desglose y sin causas:
 * {@code CartResult.descartadas()}.
 */
public enum LineVerdict {

    ACCEPTED,

    UNKNOWN_CODE,

    NOT_SELLABLE,

    NOT_SELF_SERVICE,

    DUPLICATE,

    /**
     * El codigo resuelve a una capacidad vendible de un eje que el motor dimensiona
     * el mismo ({@code CapacityHint}: usuarios, sedes). El modelo nunca ve estos
     * codigos en el prompt -{@code ProposalPromptBuilder} los excluye del bloque de
     * catalogo-, asi que verlos en {@code necesarios}/{@code recomendados} es
     * entrada no confiable y no una eleccion legitima: aceptarlos con
     * {@code quantity = 1} cobraria una unidad cuando el nucleo ya puede estar
     * regalando dos. La cifra real, si toca cobrar algo, la escribe
     * {@code ProposalCart} en su propio paso de cierre.
     */
    CAPACITY_DERIVED;

    public boolean esAceptado() {
        return this == ACCEPTED;
    }
}
