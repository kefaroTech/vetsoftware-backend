package com.vetsoftware.app.configurator.domain;

/**
 * Una pregunta {@link AnswerType#NUMBER} con opciones colgando.
 *
 * <p>
 * Son dos formas de responder que se excluyen: en una {@code NUMBER} la
 * respuesta <em>es</em> el número que escribe el cliente, y en las demás es la
 * opción que marca. Una pregunta que tiene las dos cosas no es ambigua para el
 * asistente —pintará el campo numérico— sino para <strong>los datos</strong>:
 * las opciones siguen ahí, siguen saliendo en
 * {@code GET /configurator/questionnaire} y siguen teniendo sus propios efectos
 * colgados.
 *
 * <p>
 * <strong>Se rechaza al guardar y no al cotizar</strong>, que es la decisión ya
 * escrita en {@link QuantityFromAnswerRequiresNumberQuestionException} para la
 * invariante hermana. Rechazarlo solo al cotizar convierte un error de
 * configuración de hace semanas en un 400 en la cara de un prospecto que está
 * intentando comprar y que no tiene a quién llamar — y quien lo ve no es quien
 * puede arreglarlo. {@code ConfiguratorAnswerCoherence} sigue rechazándolo
 * también al resolver, pero como red, no como diagnóstico.
 *
 * <p>
 * El mensaje es el mismo por los dos extremos —crear la opción bajo una
 * pregunta que ya es {@code NUMBER}, o pasar a {@code NUMBER} una pregunta que
 * ya tiene opciones— porque es <strong>una sola invariante</strong>. Cuál de
 * los dos caminos se intentó lo dice el endpoint que se llamó; dos códigos de
 * error obligarían al front a escribir dos tratamientos para lo mismo.
 */
public class NumberQuestionCannotHaveOptionsException extends RuntimeException {
    public NumberQuestionCannotHaveOptionsException(Long questionId, String code) {
        super("Question " + questionId + " (" + code
                + ") cannot be a NUMBER question and have options at the same time: a NUMBER"
                + " question is answered with a number, not by picking an option");
    }
}
