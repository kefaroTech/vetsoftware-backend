package com.vetsoftware.app.quote.application.dto;

import com.vetsoftware.app.quote.domain.QuoteAnswer;

/** La respuesta del configurador, con el codigo de pregunta congelado. */
public record QuoteAnswerDto(Long id, Long questionId, Long optionId, String questionCode,
        String answerValue, boolean enabled) {

    public static QuoteAnswerDto from(QuoteAnswer answer) {
        return new QuoteAnswerDto(answer.getId(), answer.getQuestionId(), answer.getOptionId(),
                answer.getQuestionCode(), answer.getAnswerValue(), answer.isEnabled());
    }
}
