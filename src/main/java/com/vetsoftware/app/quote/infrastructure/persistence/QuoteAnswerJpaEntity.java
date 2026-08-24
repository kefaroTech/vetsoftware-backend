package com.vetsoftware.app.quote.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * La respuesta del configurador que produjo la seleccion.
 *
 * <p>
 * <b>Sin {@code @Version}</b>, exenta con el codigo {@code E1_APPEND_ONLY}: se
 * inserta una vez y ahi acaba. Por eso tampoco lleva {@code @SQLDelete}.
 *
 * <p>
 * {@code questionId} y {@code optionId} son columnas planas: sus FK viven en la
 * base para poder navegar, pero el dato que importa -{@code questionCode}- esta
 * COPIADO aqui, porque el cuestionario se reescribe y esta fila tiene que
 * seguir explicando por que se vendio eso.
 */
@Entity
@Table(name = "quote_answers")
public class QuoteAnswerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "question_code", nullable = false, length = 50)
    private String questionCode;

    @Column(name = "answer_value", length = 255)
    private String answerValue;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected QuoteAnswerJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public String getQuestionCode() {
        return questionCode;
    }

    public void setQuestionCode(String questionCode) {
        this.questionCode = questionCode;
    }

    public String getAnswerValue() {
        return answerValue;
    }

    public void setAnswerValue(String answerValue) {
        this.answerValue = answerValue;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
