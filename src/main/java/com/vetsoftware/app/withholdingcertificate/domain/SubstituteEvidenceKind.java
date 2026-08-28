package com.vetsoftware.app.withholdingcertificate.domain;

/**
 * El soporte que sustituye al certificado cuando el cliente no lo expide.
 *
 * <p>
 * <strong>Una lista cerrada de un solo valor parece una constante disfrazada, y
 * no lo es.</strong> Cuando el cliente incumple su obligacion de expedir el
 * certificado, la ley permite sustituirlo por el comprobante de pago siempre
 * que reuna los datos exigidos. La factura no sirve: la emite un tercero y no
 * lleva la retencion practicada. Es decir, el conjunto de sustitutos admitidos
 * tiene exactamente un elemento, y ese hecho es una <em>decision legal</em>, no
 * una casualidad del modelo.
 *
 * <p>
 * Escribirlo como enum de un valor -y no como un {@code boolean
 * tieneSustituto}- deja dos cosas dichas por escrito: que el conjunto es
 * cerrado, y cual es la unica forma admitida hoy. El dia que la norma admita
 * otro soporte, entra aqui como una constante mas y la constraint
 * {@code chk_withholding_certificates_substitute} se amplia en el mismo cambio;
 * con un booleano no habria donde ponerlo ni quien avisara de que falta.
 */
public enum SubstituteEvidenceKind {

    /** Comprobante de pago. El unico sustituto que la ley admite. */
    PAYMENT_RECEIPT
}
