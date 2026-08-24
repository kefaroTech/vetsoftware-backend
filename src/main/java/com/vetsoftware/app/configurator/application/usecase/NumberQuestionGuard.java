package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.configurator.domain.NumberQuestionCannotHaveOptionsException;

/**
 * Una pregunta {@code NUMBER} no tiene opciones, y se comprueba <strong>al
 * guardar</strong>.
 *
 * <p>
 * La comprobación cruza dos filas de dos tablas —la opción y el
 * {@code answer_type} de su pregunta— así que ningún {@code CHECK} de MySQL
 * puede con ella, y {@code ConfiguratorOption} tampoco: la entidad no conoce el
 * tipo de respuesta de la pregunta a la que pertenece. Es la misma forma que
 * {@link QuantityFromAnswerGuard}, por la misma razón.
 *
 * <p>
 * <strong>Los dos extremos viven aquí y no repartidos por los
 * servicios.</strong> Es una sola invariante y tiene dos puertas de entrada:
 * crear una opción bajo una pregunta que ya es {@code NUMBER}, y pasar a
 * {@code NUMBER} una pregunta que ya tiene opciones. Escribir la comprobación
 * dos veces en dos servicios es exactamente cómo una de las dos se queda sin
 * ella — que fue el defecto del issue #434, donde el guardián miraba solo desde
 * el lado del efecto y el lado de la pregunta pasaba sin mirar nada.
 *
 * <p>
 * {@code ConfiguratorAnswerCoherence} rechaza la misma incoherencia al resolver
 * (issue #436). Ese rechazo <strong>no sobra y este no lo sustituye</strong>:
 * aquel protege la cotización de un cuerpo manipulado, este le da el
 * diagnóstico a quien puede arreglarlo, semanas antes y en el endpoint que
 * tocó.
 *
 * <p>
 * Estático y con la pregunta ya cargada o el puerto por parámetro, como
 * {@link ConditionalQuestionGuard}: no es un caso de uso y no debe ser un bean
 * más que alguien pueda inyectar por error donde toca un {@code UseCase}.
 */
final class NumberQuestionGuard {

    private NumberQuestionGuard() {
    }

    /**
     * Al crear una opción: la pregunta a la que se cuelga no puede ser
     * {@code NUMBER}.
     *
     * <p>
     * Recibe la {@link ConfiguratorQuestion} ya cargada y no su id a propósito.
     * {@code CreateConfiguratorOptionService} tiene que traerla de todos modos para
     * dar un 404 con nombre si no existe, así que pedirla por id aquí significaría
     * la misma consulta dos veces en la misma transacción para no aprovechar una
     * fila que el llamador ya tiene en la mano.
     */
    static void assertQuestionAdmitsOptions(ConfiguratorQuestion question) {
        if (question.getAnswerType() == AnswerType.NUMBER) {
            throw new NumberQuestionCannotHaveOptionsException(question.getId(),
                    question.getCode());
        }
    }

    /**
     * <strong>La misma invariante, mirada desde el otro lado.</strong>
     * {@link #assertQuestionAdmitsOptions} la vigila cuando se toca la opción; esto
     * la vigila cuando se toca la pregunta.
     *
     * <p>
     * Sin ella, pasar una pregunta {@code SINGLE} con opciones a {@code NUMBER}
     * responde 200 y deja las opciones vivas en la base. El cuestionario público
     * las sigue devolviendo colgadas de una pregunta que el asistente pinta como
     * campo numérico, y el día que alguien las manda —porque el front las pinta, o
     * porque un prospecto reenvía un cuerpo anterior— {@code POST
     * /configurator/resolve} responde 400 y el asistente se queda bloqueado. Se
     * descubre perdiendo la venta.
     *
     * <p>
     * <strong>Deja las dos salidas abiertas a propósito.</strong> Solo se mira el
     * paso <em>hacia</em> {@code NUMBER}: un cuestionario que ya esté mal se
     * arregla dando de baja las opciones —que no pasa por aquí— o devolviendo la
     * pregunta a {@code SINGLE}, que esta comprobación deja pasar sin mirar nada.
     * Un guardián que cerrase también la vuelta dejaría esos datos sin ninguna
     * salida por la API.
     *
     * <p>
     * Consulta {@code existsByQuestionId}, que ya existía y ya lo llama
     * {@code DeleteConfiguratorQuestionService}: esta invariante no añade ni un
     * método al puerto. No es un detalle de estilo — el issue #438 está abierto en
     * este mismo slice justamente por métodos de puerto sin llamador de producción,
     * «con el nombre que invita a reintroducir el defecto».
     */
    static void assertNoOptionsInTheWay(Long questionId, String code, AnswerType nuevoTipo,
            ConfiguratorOptionRepository options) {
        if (nuevoTipo != AnswerType.NUMBER) {
            return;
        }
        if (options.existsByQuestionId(questionId)) {
            throw new NumberQuestionCannotHaveOptionsException(questionId, code);
        }
    }
}
