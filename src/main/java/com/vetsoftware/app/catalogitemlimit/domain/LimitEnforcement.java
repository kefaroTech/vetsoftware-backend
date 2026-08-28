package com.vetsoftware.app.catalogitemlimit.domain;

/**
 * Qué hacer al llegar al tope.
 *
 * <p>
 * <strong>Hoy el único comportamiento posible es el portazo, cableado dentro de
 * una consulta</strong>, y por eso declarar «solo avisar» bloquea igual — y el
 * cliente no distingue eso de un fallo. Este enum es la mitad declarativa del
 * arreglo: la otra mitad es que el modo viaje dentro de la misma instrucción
 * que comprueba el techo.
 */
public enum LimitEnforcement {

    /** Avisa y deja crear. */
    WARN,

    /** No deja crear. Es la palanca que hace que el cliente pase a pagar. */
    BLOCK,

    /** Baja a consulta. */
    READ_ONLY,

    /**
     * Deja pasar y lo cobra. Es la excepción, no la regla: solo donde bloquear
     * haría daño real —facturar es obligación tributaria de la clínica y la sanción
     * le cae a ella—.
     */
    OVERAGE;

    /** Solo el excedente exige precio por unidad, y lo exige siempre. */
    public boolean requiresOveragePrice() {
        return this == OVERAGE;
    }

    /** Si el intento de crear por encima del techo se deja pasar. */
    public boolean allowsCreationOverLimit() {
        return this == WARN || this == OVERAGE;
    }
}
