package com.vetsoftware.app.entitlement.domain;

/**
 * Que puede hacer una empresa con un submodulo <em>ahora mismo</em>.
 *
 * <p>
 * El orden de declaracion es el orden de restriccion, de menos a mas, y
 * {@link #restrictedTo(AccessLevel)} depende de el: FULL es uso normal,
 * READ_ONLY es consultar e imprimir --ni crear ni modificar-- y NONE significa
 * <em>"ese modulo no existe para el"</em>.
 *
 * <p>
 * <strong>No hay, ni debe anadirse, un valor que signifique
 * "bloqueado"</strong> (R18 de {@code suscripciones-reglas-codigo.md}). El
 * maximo grado de restriccion es {@link #READ_ONLY}, incluso para un moroso:
 * cortarle el acceso a su propia historia clinica es riesgo legal, no una
 * palanca de cobranza.
 */
public enum AccessLevel {
    FULL, READ_ONLY, NONE;

    /** Puede crear y modificar. */
    public boolean allowsWrite() {
        return this == FULL;
    }

    /** Puede consultar e imprimir: todo salvo el submodulo oculto. */
    public boolean allowsRead() {
        return this != NONE;
    }

    /** El mas restrictivo de los dos: el techo del contrato gana siempre. */
    public AccessLevel restrictedTo(AccessLevel ceiling) {
        return ordinal() >= ceiling.ordinal() ? this : ceiling;
    }

    /**
     * Un submodulo que no sabe funcionar en solo lectura no se degrada: se
     * <strong>oculta</strong>. Ensenar pantallas a medias, con los botones vivos y
     * el guardado rechazado, es peor que no ensenarlas.
     */
    public AccessLevel hiddenIfNotReadOnlyCapable(boolean readOnlyCapable) {
        return this == READ_ONLY && !readOnlyCapable ? NONE : this;
    }
}
