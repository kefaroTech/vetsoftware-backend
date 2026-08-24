package com.vetsoftware.app.configurator.application.dto;

import com.vetsoftware.app.configurator.domain.AnswerType;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @param options
 *            las opciones activas de la pregunta, anidadas. Existen aqui porque
 *            sin ellas el editor del cuestionario tenia que pedir
 *            {@code /questions/{id}/options} una vez por pregunta -1 + 1 + N
 *            peticiones por carga Y por guardado, mas de 400 al ajustar diez
 *            efectos- (incidencia #448). Nunca es {@code null}: una pregunta
 *            {@code NUMBER} tiene la lista vacia, que es un dato y no una
 *            ausencia.
 */
public record ConfiguratorQuestionDto(Long id, String code, String questionText, String helpText,
        AnswerType answerType, Long parentOptionId, boolean required, int sortOrder,
        LocalDateTime createdDate, boolean enabled, List<ConfiguratorOptionDto> options) {

    public ConfiguratorQuestionDto {
        options = options == null ? List.of() : List.copyOf(options);
    }

    /** Sin opciones resueltas. */
    public ConfiguratorQuestionDto(Long id, String code, String questionText, String helpText,
            AnswerType answerType, Long parentOptionId, boolean required, int sortOrder,
            LocalDateTime createdDate, boolean enabled) {
        this(id, code, questionText, helpText, answerType, parentOptionId, required, sortOrder,
                createdDate, enabled, List.of());
    }

    public static ConfiguratorQuestionDto from(ConfiguratorQuestion question) {
        return from(question, List.of());
    }

    public static ConfiguratorQuestionDto from(ConfiguratorQuestion question,
            List<ConfiguratorOption> options) {
        return new ConfiguratorQuestionDto(question.getId(), question.getCode(),
                question.getQuestionText(), question.getHelpText(), question.getAnswerType(),
                question.getParentOptionId(), question.isRequired(), question.getSortOrder(),
                question.getCreatedDate(), question.isEnabled(),
                options.stream().map(ConfiguratorOptionDto::from).toList());
    }
}
