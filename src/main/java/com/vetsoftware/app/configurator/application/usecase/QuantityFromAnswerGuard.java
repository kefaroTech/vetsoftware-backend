package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorOptionNotFoundException;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestionNotFoundException;
import com.vetsoftware.app.configurator.domain.EffectType;
import com.vetsoftware.app.configurator.domain.QuantityFromAnswerRequiresNumberQuestionException;

/**
 * La segunda trampa: {@code QUANTITY_FROM_ANSWER} colgado de algo que no es una
 * pregunta {@code NUMBER}.
 *
 * <p>
 * La comprobación cruza dos filas de dos tablas —el efecto y la pregunta que lo
 * dispara— así que ningún {@code CHECK} de MySQL puede con ella y ninguna
 * invariante de {@code ConfiguratorEffect} tampoco: la entidad no conoce el
 * tipo de respuesta de su pregunta.
 *
 * <p>
 * <strong>Sube por el disparador, sea cual sea.</strong> Si el efecto lo
 * dispara una opción, la pregunta que se mira es la dueña de esa opción — y una
 * pregunta con opciones nunca es {@code NUMBER}, así que ese caso queda
 * rechazado por el mismo camino sin necesidad de una regla aparte.
 */
final class QuantityFromAnswerGuard {

    private QuantityFromAnswerGuard() {
    }

    static void assertCoherent(EffectType effect, Long optionId, Long questionId,
            ConfiguratorQuestionRepository questions, ConfiguratorOptionRepository options) {
        if (effect != EffectType.QUANTITY_FROM_ANSWER) {
            return;
        }
        Long preguntaDisparadora = questionId != null
                ? questionId
                : questionIdOf(optionId, options);
        ConfiguratorQuestion pregunta = questions.findById(preguntaDisparadora)
                .orElseThrow(() -> new ConfiguratorQuestionNotFoundException(preguntaDisparadora));
        if (pregunta.getAnswerType() != AnswerType.NUMBER) {
            throw new QuantityFromAnswerRequiresNumberQuestionException(pregunta.getId(),
                    pregunta.getAnswerType());
        }
    }

    /**
     * <strong>La misma invariante, mirada desde el otro lado.</strong>
     * {@link #assertCoherent} la vigila cuando se toca el efecto; esto la vigila
     * cuando se toca la pregunta.
     *
     * <p>
     * Sin ella, pasar una pregunta de {@code NUMBER} a {@code SINGLE} responde 200
     * y deja vivo el efecto {@code QUANTITY_FROM_ANSWER} que colgaba de ella. El
     * asistente deja de pintar el campo numérico, el front deja de mandar
     * {@code numericAnswers} para esa pregunta,
     * {@code ConfiguratorResolver.seDispara} devuelve {@code false} y el efecto no
     * vuelve a dispararse jamás: todas las cotizaciones salen sin ese artículo, sin
     * excepción, sin log y sin línea de cero unidades. Se descubre facturando de
     * menos.
     *
     * <p>
     * Se rechaza la edición en vez de deshabilitar los efectos en cascada, que es
     * el mismo criterio con el que {@code DeleteConfiguratorQuestionService} no
     * deja dar de baja una pregunta con hijos vivos: quien edita ve el conflicto y
     * decide, en vez de descubrir meses después que algo se apagó solo.
     *
     * <p>
     * La excepción es la <em>misma</em> que la del otro lado a propósito: al front
     * le da igual desde qué endpoint se rompió la coherencia, y dos códigos de
     * error para una única invariante le obligarían a escribir dos veces el mismo
     * tratamiento.
     */
    static void assertQuestionTypeStillFits(Long questionId, AnswerType nuevoTipo,
            ConfiguratorEffectRepository effects) {
        if (nuevoTipo == AnswerType.NUMBER) {
            return;
        }
        if (effects.existsQuantityFromAnswerByQuestionId(questionId)) {
            throw new QuantityFromAnswerRequiresNumberQuestionException(questionId, nuevoTipo);
        }
    }

    private static Long questionIdOf(Long optionId, ConfiguratorOptionRepository options) {
        ConfiguratorOption option = options.findById(optionId)
                .orElseThrow(() -> new ConfiguratorOptionNotFoundException(optionId));
        return option.getQuestionId();
    }
}
