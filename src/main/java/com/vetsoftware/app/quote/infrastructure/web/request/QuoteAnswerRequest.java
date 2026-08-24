package com.vetsoftware.app.quote.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuoteAnswerRequest(@NotNull Long questionId, Long optionId,
        @Size(max = 255) String answerValue) {
}
