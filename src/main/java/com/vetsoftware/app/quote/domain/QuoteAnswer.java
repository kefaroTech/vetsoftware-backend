package com.vetsoftware.app.quote.domain;

import java.time.LocalDateTime;

/**
 * La respuesta del configurador que produjo la seleccion, con el codigo de la
 * pregunta COPIADO.
 *
 * <p>
 * Parece accesorio y no lo es: es la unica forma de responder "por que le
 * vendimos esto" seis meses despues, cuando el cuestionario ya se reescribio y
 * la pregunta 14 pregunta otra cosa. Por eso se copia {@code questionCode}: el
 * {@code questionId} solo sirve para navegar.
 */
public class QuoteAnswer {

    private static final int MAX_CODE = 50;
    private static final int MAX_VALUE = 255;

    private final Long id;
    private final Long questionId;
    private final Long optionId;
    private final String questionCode;
    private final String answerValue;
    private final LocalDateTime createdDate;
    private final boolean enabled;

    public QuoteAnswer(Long id, Long questionId, Long optionId, String questionCode,
            String answerValue, LocalDateTime createdDate, boolean enabled) {
        if (questionId == null)
            throw new IllegalArgumentException("questionId is required");
        if (questionCode == null || questionCode.isBlank())
            throw new IllegalArgumentException("questionCode is required");
        if (questionCode.length() > MAX_CODE)
            throw new IllegalArgumentException("questionCode must be 50 chars or less");
        if (answerValue != null && answerValue.length() > MAX_VALUE)
            throw new IllegalArgumentException("answerValue must be 255 chars or less");
        // chk_quote_answers_payload: una respuesta sin opcion y sin valor no dice
        // nada, y guardarla solo ensucia el informe que esta tabla existe para dar.
        if (optionId == null && (answerValue == null || answerValue.isBlank()))
            throw new IllegalArgumentException("answer requires optionId or answerValue");
        this.id = id;
        this.questionId = questionId;
        this.optionId = optionId;
        this.questionCode = questionCode;
        this.answerValue = answerValue;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    /** Registra una respuesta nueva copiando el codigo de la pregunta. */
    public static QuoteAnswer capture(ConfiguratorQuestionRef question, Long optionId,
            String answerValue, LocalDateTime createdDate) {
        if (question == null)
            throw new IllegalArgumentException("configurator question is required");
        return new QuoteAnswer(null, question.id(), optionId, question.code(), answerValue,
                createdDate, true);
    }

    public Long getId() {
        return id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public String getQuestionCode() {
        return questionCode;
    }

    public String getAnswerValue() {
        return answerValue;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
