package com.vetsoftware.app.configurator.domain;

/**
 * La segunda trampa de árbol.
 *
 * <p>
 * {@code QUANTITY_FROM_ANSWER} significa «usa como cantidad el número que
 * escribió el cliente». Si la pregunta que dispara el efecto no es
 * {@link AnswerType#NUMBER} no hay ningún número: la respuesta es una opción.
 * El efecto queda escrito, se ve razonable en la consola, y el día que un
 * prospecto llega por esa rama la resolución no encuentra cantidad — o la
 * inventa.
 *
 * <p>
 * Se rechaza <strong>al guardar el efecto</strong> y no al cotizar. Rechazarlo
 * al cotizar convierte un error de configuración de hace meses en un fallo en
 * la cara del cliente, y quien lo ve no es quien puede arreglarlo.
 */
public class QuantityFromAnswerRequiresNumberQuestionException extends RuntimeException {
    public QuantityFromAnswerRequiresNumberQuestionException(Long questionId,
            AnswerType answerType) {
        super("QUANTITY_FROM_ANSWER requires a NUMBER question, but question " + questionId + " is "
                + answerType);
    }
}
