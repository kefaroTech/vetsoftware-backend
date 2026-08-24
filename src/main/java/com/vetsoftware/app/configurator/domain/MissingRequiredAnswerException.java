package com.vetsoftware.app.configurator.domain;

/**
 * Una pregunta obligatoria cuya rama sí está activa y que llegó sin responder.
 *
 * <p>
 * Es la mitad simétrica de {@link UnreachableAnswerException}, y sin ella la
 * manipulación se hace <strong>por omisión</strong> en vez de por adición:
 * quien quiera saltarse un artículo que el cuestionario le impone —el núcleo
 * obligatorio, por ejemplo— no necesita añadir nada, le basta con no mandar la
 * respuesta que lo dispara. Cerrar solo la adición deja la puerta de al lado
 * abierta.
 *
 * <p>
 * Expone la pregunta como datos por el mismo motivo que su gemela: el mensaje
 * sale en inglés nombrando un id interno y lo pinta un aviso en español a quien
 * no puede actuar sobre «Required question 12». Ver incidencia #449.
 */
public class MissingRequiredAnswerException extends RuntimeException {

    private final Long questionId;
    private final String questionCode;

    public MissingRequiredAnswerException(Long questionId, String code) {
        super("Required question " + questionId + " (" + code + ") has no answer");
        this.questionId = questionId;
        this.questionCode = code;
    }

    public Long getQuestionId() {
        return questionId;
    }

    /** El código de negocio de la pregunta que falta por responder. */
    public String getQuestionCode() {
        return questionCode;
    }
}
