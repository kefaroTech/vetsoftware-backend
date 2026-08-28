package com.vetsoftware.app.externalinvoicingoutage.domain;

/**
 * Quien causo la caida de la emision fiscal.
 *
 * <p>
 * <strong>Es la columna que separa un incidente de un incumplimiento</strong>,
 * y por eso no es texto libre sino una lista cerrada: espejo exacto de
 * {@code chk_eio_cause}. Ante la autoridad, «se cayo el emisor externo» y «se
 * cayo lo nuestro» son dos posiciones juridicas distintas, y la diferencia
 * entre las dos no puede depender de como redacto el resumen quien abrio la
 * ficha.
 *
 * <p>
 * <strong>Ademas decide la unicidad.</strong> La columna generada
 * {@code open_outage_marker} vale este mismo codigo mientras la caida sigue
 * abierta, de modo que {@code uq_eio_open} admite <b>una sola caida abierta por
 * causante</b>. Dos caidas simultaneas del emisor externo son la misma caida;
 * dos causantes distintos —el emisor y la red— si pueden solaparse.
 */
public enum CauseParty {
    /** El proveedor tecnologico que transmite a la DIAN. */
    EXTERNAL_ISSUER,
    /** La propia autoridad: sus servicios de validacion no responden. */
    AUTHORITY,
    /** El transporte entre medias, sin culpa de ninguno de los dos extremos. */
    NETWORK,
    /**
     * Nuestro. El unico valor que convierte la ficha en la prueba de un
     * incumplimiento propio, y por eso tiene que poder escribirse.
     */
    OWN
}
