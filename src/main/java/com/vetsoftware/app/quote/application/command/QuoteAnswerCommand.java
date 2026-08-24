package com.vetsoftware.app.quote.application.command;

/** Una respuesta del configurador tal como se dio. */
public record QuoteAnswerCommand(Long questionId, Long optionId, String answerValue) {
}
