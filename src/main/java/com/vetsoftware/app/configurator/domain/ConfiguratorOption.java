package com.vetsoftware.app.configurator.domain;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Una respuesta posible a una pregunta de opción.
 *
 * <p>
 * El {@code code} es único <em>dentro de su pregunta</em> y no global
 * ({@code uq_configurator_options_code} es {@code (question_id, code)}): dos
 * preguntas distintas pueden tener cada una su opción {@code YES}, que es lo
 * normal en un cuestionario.
 */
public class ConfiguratorOption {

    private static final int CODE_MAX = 50;
    private static final int LABEL_MAX = 255;
    private static final int HELP_TEXT_MAX = 500;

    private final Long id;
    private final Long questionId;
    private final String code;
    private String label;
    private String helpText;
    private int sortOrder;
    private final LocalDateTime createdDate;
    private final Long version;
    private boolean enabled;

    public ConfiguratorOption(Long id, Long questionId, String code, String label, String helpText,
            int sortOrder, LocalDateTime createdDate, Long version, boolean enabled) {
        validate(questionId, code, label, helpText, sortOrder);
        this.id = id;
        this.questionId = questionId;
        this.code = code;
        this.label = label;
        this.helpText = helpText;
        this.sortOrder = sortOrder;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static ConfiguratorOption create(Long questionId, String code, String label,
            String helpText, int sortOrder, Clock clock) {
        return new ConfiguratorOption(null, questionId, code, label, helpText, sortOrder,
                LocalDateTime.now(clock), null, true);
    }

    /**
     * Ni el {@code code} ni la pregunta a la que pertenece son editables: mover una
     * opción de pregunta reescribiría el sentido de las respuestas ya guardadas en
     * {@code quote_answers} y de los efectos que cuelgan de ella.
     */
    public void update(String label, String helpText, int sortOrder) {
        validate(this.questionId, this.code, label, helpText, sortOrder);
        this.label = label;
        this.helpText = helpText;
        this.sortOrder = sortOrder;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    private static void validate(Long questionId, String code, String label, String helpText,
            int sortOrder) {
        if (questionId == null)
            throw new IllegalArgumentException("questionId is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > CODE_MAX)
            throw new IllegalArgumentException("code must be " + CODE_MAX + " chars or less");
        if (label == null || label.isBlank())
            throw new IllegalArgumentException("label is required");
        if (label.length() > LABEL_MAX)
            throw new IllegalArgumentException("label must be " + LABEL_MAX + " chars or less");
        if (helpText != null && helpText.length() > HELP_TEXT_MAX)
            throw new IllegalArgumentException(
                    "helpText must be " + HELP_TEXT_MAX + " chars or less");
        if (sortOrder < 0)
            throw new IllegalArgumentException("sortOrder cannot be negative");
    }

    public Long getId() {
        return id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getHelpText() {
        return helpText;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
