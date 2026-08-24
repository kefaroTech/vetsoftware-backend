package com.vetsoftware.app.configurator.domain;

/**
 * La primera trampa de árbol del configurador.
 *
 * <p>
 * {@code configurator_questions.parent_option_id} forma un árbol de preguntas
 * condicionales: «¿cuántas cajas?» solo aparece si antes se dijo que se cobra
 * en mostrador. La base impone que el padre exista, no que la ascendencia
 * termine. Si A cuelga de una opción de B y B acaba colgando de una opción de
 * A, el recorrido del cuestionario no tiene raíz: el asistente no puede decidir
 * qué preguntar primero y el prospecto se queda con la página girando.
 *
 * <p>
 * No es expresable en MySQL —un {@code CHECK} no recorre filas— así que se
 * comprueba al guardar, que es el único momento en que el arco todavía no
 * existe.
 */
public class ConditionalQuestionCycleException extends RuntimeException {
    public ConditionalQuestionCycleException(String message) {
        super(message);
    }
}
