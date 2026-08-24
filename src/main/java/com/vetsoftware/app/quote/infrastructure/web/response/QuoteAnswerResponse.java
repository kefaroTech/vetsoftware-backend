package com.vetsoftware.app.quote.infrastructure.web.response;

public record QuoteAnswerResponse(Long id, Long questionId, Long optionId, String questionCode,
        String answerValue, boolean enabled) {
}
