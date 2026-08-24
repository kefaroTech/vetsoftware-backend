package com.vetsoftware.app.configurator.domain;

/**
 * Cómo se responde una pregunta del asistente de venta.
 *
 * <p>
 * El dominio cerrado lo impone además
 * {@code chk_configurator_questions_answer_type} en la base. Aquí importa
 * porque {@link EffectType#QUANTITY_FROM_ANSWER} solo tiene sentido colgado de
 * una pregunta {@link #NUMBER}: en las demás la respuesta es una opción, y de
 * una opción no sale ningún número que meter en el carrito.
 */
public enum AnswerType {
    /** Una sola opción de una lista. */
    SINGLE,
    /** Varias opciones de una lista. */
    MULTI,
    /** Un número escrito por el cliente. La respuesta <em>es</em> la cantidad. */
    NUMBER,
    /** Sí o no, modelado como dos opciones. */
    BOOLEAN
}
