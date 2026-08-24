package com.vetsoftware.app.configurator.domain;

/**
 * Una respuesta a una pregunta que el cuestionario nunca llegó a mostrar.
 *
 * <p>
 * Es el intento de manipulación con salida a dinero: el prospecto responde «no
 * cobro en mostrador» y aun así manda en el JSON el {@code optionId} de la rama
 * de caja. Sin esta comprobación el efecto se dispara igual, {@code quote}
 * convierte la selección en líneas con precio congelado, la cotización aceptada
 * se vuelve contrato y el contrato devenga cargos. Para cuando se detecta ya no
 * se corrige editando: hay que emitir una nota crédito.
 *
 * <p>
 * <strong>Se rechaza, no se descarta.</strong> Ignorar la respuesta huérfana en
 * silencio produciría un carrito distinto del que pidió el cliente sin que
 * nadie se entere de que hubo un intento — que es peor que el error, porque no
 * deja rastro ni de la manipulación ni del front que la provocó por un fallo
 * suyo.
 *
 * <p>
 * <strong>Los datos viajan en campos, no solo dentro de la frase.</strong> El
 * mensaje está en inglés y nombra ids internos, y lo pinta tal cual un aviso en
 * español —a un operador de la consola, y también al prospecto anónimo, porque
 * {@code POST /configurator/resolve} es público por diseño—. «Answer refers to
 * option 42…» no le dice a nadie qué hacer. Con {@code questionId},
 * {@code questionCode} y {@code optionId} como propiedades del
 * {@code ProblemDetail}, el cliente escribe la frase en español nombrando la
 * pregunta con las mismas palabras que hay en pantalla, sin volver a preguntar
 * al servidor. Incidencia #449.
 */
public class UnreachableAnswerException extends RuntimeException {

    private final Long questionId;
    private final String questionCode;
    private final Long optionId;

    /**
     * Sin datos estructurados. Se conserva para los casos en que de verdad no hay
     * ninguno que dar; toda ruta que sí los tenga debe usar el constructor
     * completo, porque una propiedad ausente obliga al cliente a volver al texto.
     */
    public UnreachableAnswerException(String message) {
        this(message, null, null, null);
    }

    public UnreachableAnswerException(String message, Long questionId, String questionCode,
            Long optionId) {
        super(message);
        this.questionId = questionId;
        this.questionCode = questionCode;
        this.optionId = optionId;
    }

    /** La pregunta implicada, si el rechazo señala a una. */
    public Long getQuestionId() {
        return questionId;
    }

    /**
     * El código de negocio de esa pregunta —{@code CAJAS}, {@code TERMINAL}—, que
     * es lo único de aquí que el operador reconoce sin abrir el editor.
     */
    public String getQuestionCode() {
        return questionCode;
    }

    /**
     * La opción implicada: la que no existe, la que sobra o la que tendría que
     * haberse marcado para que la rama estuviera viva.
     */
    public Long getOptionId() {
        return optionId;
    }
}
