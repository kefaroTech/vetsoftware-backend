package com.vetsoftware.app.configurator.domain;

/**
 * Qué le hace una respuesta al carrito.
 *
 * <p>
 * Es el vocabulario que convierte el cuestionario en datos: cambiar lo que
 * vende una campaña es insertar filas de {@code configurator_effects}, no
 * desplegar código.
 */
public enum EffectType {
    /** Mete el artículo con cantidad 1 si no estaba. */
    ADD,
    /** Saca el artículo del carrito. */
    REMOVE,
    /** Fija una cantidad literal, escrita en la propia fila del efecto. */
    SET_QUANTITY,
    /**
     * Fija como cantidad el número que respondió el cliente. Solo es coherente
     * disparado por una pregunta {@link AnswerType#NUMBER}, y eso lo comprueba el
     * caso de uso al guardar el efecto — nunca al cotizar, que es demasiado tarde.
     */
    QUANTITY_FROM_ANSWER
}
