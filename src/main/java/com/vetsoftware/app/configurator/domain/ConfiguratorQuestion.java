package com.vetsoftware.app.configurator.domain;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Una pregunta del asistente de venta.
 *
 * <p>
 * <strong>Ninguna pregunta está escrita en el programa.</strong> Cambiar el
 * cuestionario —o lanzar una campaña— es editar filas desde la consola, no
 * desplegar. Por eso el texto, el orden y la condición de aparición son datos.
 *
 * <p>
 * {@code parentOptionId} es la condición: la pregunta solo se muestra si el
 * cliente eligió esa opción. La columna se añade en el paso 9 de la migración
 * con un {@code ALTER}, porque {@code configurator_questions} y
 * {@code configurator_options} se apuntan mutuamente y MySQL exige que la tabla
 * referenciada exista. Aquí se mapea como un {@code Long} pelado y no como una
 * asociación JPA justamente para que ese ciclo no vuelva a aparecer del lado de
 * Hibernate.
 */
public class ConfiguratorQuestion {

    private static final int CODE_MAX = 50;
    private static final int QUESTION_TEXT_MAX = 255;
    private static final int HELP_TEXT_MAX = 500;

    private final Long id;
    private final String code;
    private String questionText;
    private String helpText;
    private AnswerType answerType;
    private Long parentOptionId;
    private boolean required;
    private int sortOrder;
    private final LocalDateTime createdDate;
    private final Long version;
    private boolean enabled;

    public ConfiguratorQuestion(Long id, String code, String questionText, String helpText,
            AnswerType answerType, Long parentOptionId, boolean required, int sortOrder,
            LocalDateTime createdDate, Long version, boolean enabled) {
        validate(code, questionText, helpText, answerType, sortOrder);
        this.id = id;
        this.code = code;
        this.questionText = questionText;
        this.helpText = helpText;
        this.answerType = answerType;
        this.parentOptionId = parentOptionId;
        this.required = required;
        this.sortOrder = sortOrder;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static ConfiguratorQuestion create(String code, String questionText, String helpText,
            AnswerType answerType, Long parentOptionId, boolean required, int sortOrder,
            Clock clock) {
        return new ConfiguratorQuestion(null, code, questionText, helpText, answerType,
                parentOptionId, required, sortOrder, LocalDateTime.now(clock), null, true);
    }

    /**
     * El {@code code} no se toca: es la referencia estable que copian
     * {@code quote_answers.question_code} y los informes comerciales. Cambiarlo
     * dejaría de significar lo mismo en cotizaciones ya emitidas.
     */
    public void update(String questionText, String helpText, AnswerType answerType,
            Long parentOptionId, boolean required, int sortOrder) {
        validate(this.code, questionText, helpText, answerType, sortOrder);
        this.questionText = questionText;
        this.helpText = helpText;
        this.answerType = answerType;
        this.parentOptionId = parentOptionId;
        this.required = required;
        this.sortOrder = sortOrder;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    /** {@code true} si la pregunta solo se muestra al responder cierta opción. */
    public boolean isConditional() {
        return parentOptionId != null;
    }

    private static void validate(String code, String questionText, String helpText,
            AnswerType answerType, int sortOrder) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > CODE_MAX)
            throw new IllegalArgumentException("code must be " + CODE_MAX + " chars or less");
        if (questionText == null || questionText.isBlank())
            throw new IllegalArgumentException("questionText is required");
        if (questionText.length() > QUESTION_TEXT_MAX)
            throw new IllegalArgumentException(
                    "questionText must be " + QUESTION_TEXT_MAX + " chars or less");
        if (helpText != null && helpText.length() > HELP_TEXT_MAX)
            throw new IllegalArgumentException(
                    "helpText must be " + HELP_TEXT_MAX + " chars or less");
        if (answerType == null)
            throw new IllegalArgumentException("answerType is required");
        if (sortOrder < 0)
            throw new IllegalArgumentException("sortOrder cannot be negative");
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getHelpText() {
        return helpText;
    }

    public AnswerType getAnswerType() {
        return answerType;
    }

    public Long getParentOptionId() {
        return parentOptionId;
    }

    public boolean isRequired() {
        return required;
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
