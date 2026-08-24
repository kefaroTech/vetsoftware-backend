package com.vetsoftware.app.configurator.application.command;

import com.vetsoftware.app.configurator.domain.AnswerType;

/**
 * Sin {@code companyId}: el cuestionario es de la plataforma y ninguna de las
 * tres tablas del configurador lleva empresa.
 */
public record CreateConfiguratorQuestionCommand(String code, String questionText, String helpText,
        AnswerType answerType, Long parentOptionId, boolean required, int sortOrder) {
}
