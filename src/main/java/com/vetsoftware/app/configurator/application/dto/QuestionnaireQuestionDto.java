package com.vetsoftware.app.configurator.application.dto;

import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import java.util.List;

/**
 * Una pregunta con sus opciones, tal como la ve un prospecto sin autenticar.
 *
 * <p>
 * Lleva {@code parentOptionId} porque es lo que permite al asistente decidir
 * cuándo mostrarla: sin él, el front tendría que pedir el cuestionario otra vez
 * cada vez que el usuario marca algo.
 */
public record QuestionnaireQuestionDto(Long id, String code, String questionText, String helpText,
        String answerType, Long parentOptionId, boolean required, int sortOrder,
        List<QuestionnaireOptionDto> options) {

    public QuestionnaireQuestionDto {
        options = options == null ? List.of() : List.copyOf(options);
    }

    public static QuestionnaireQuestionDto from(ConfiguratorQuestion question,
            List<QuestionnaireOptionDto> options) {
        return new QuestionnaireQuestionDto(question.getId(), question.getCode(),
                question.getQuestionText(), question.getHelpText(), question.getAnswerType().name(),
                question.getParentOptionId(), question.isRequired(), question.getSortOrder(),
                options);
    }
}
