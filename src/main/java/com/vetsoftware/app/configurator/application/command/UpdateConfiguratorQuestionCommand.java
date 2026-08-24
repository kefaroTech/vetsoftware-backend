package com.vetsoftware.app.configurator.application.command;

import com.vetsoftware.app.configurator.domain.AnswerType;

public record UpdateConfiguratorQuestionCommand(Long id, String questionText, String helpText,
        AnswerType answerType, Long parentOptionId, boolean required, int sortOrder) {
}
