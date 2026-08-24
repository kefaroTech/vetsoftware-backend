package com.vetsoftware.app.configurator.domain;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * El corazón del configurador: traduce una respuesta en un artículo del
 * carrito.
 *
 * <p>
 * <strong>Se dispara por opción o por pregunta, nunca por las dos.</strong> Por
 * opción cuando la respuesta es una elección; por pregunta cuando la pregunta
 * es numérica y el número <em>es</em> la respuesta. Con los dos disparadores
 * rellenos el efecto se ejecutaría dos veces y el artículo entraría duplicado
 * en el carrito — de ahí {@code chk_configurator_effects_trigger} en la base y
 * la misma invariante repetida aquí, que es la que da un 400 con mensaje en vez
 * de un 500 traduciendo una violación de constraint.
 */
public class ConfiguratorEffect {

    private final Long id;
    private final Long optionId;
    private final Long questionId;
    private Long catalogItemId;
    private EffectType effect;
    private Integer quantity;
    private final LocalDateTime createdDate;
    private final Long version;
    private boolean enabled;

    public ConfiguratorEffect(Long id, Long optionId, Long questionId, Long catalogItemId,
            EffectType effect, Integer quantity, LocalDateTime createdDate, Long version,
            boolean enabled) {
        validate(optionId, questionId, catalogItemId, effect, quantity);
        this.id = id;
        this.optionId = optionId;
        this.questionId = questionId;
        this.catalogItemId = catalogItemId;
        this.effect = effect;
        this.quantity = quantity;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static ConfiguratorEffect create(Long optionId, Long questionId, Long catalogItemId,
            EffectType effect, Integer quantity, Clock clock) {
        return new ConfiguratorEffect(null, optionId, questionId, catalogItemId, effect, quantity,
                LocalDateTime.now(clock), null, true);
    }

    /**
     * El disparador no se edita. Cambiar de opción a pregunta —o de una opción a
     * otra— es otro efecto distinto, y editarlo en sitio deja las dos claves únicas
     * de la tabla vigilando pares que ya no existen.
     */
    public void update(Long catalogItemId, EffectType effect, Integer quantity) {
        validate(this.optionId, this.questionId, catalogItemId, effect, quantity);
        this.catalogItemId = catalogItemId;
        this.effect = effect;
        this.quantity = quantity;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    /** {@code true} si lo dispara el número respondido a una pregunta. */
    public boolean isTriggeredByQuestion() {
        return questionId != null;
    }

    private static void validate(Long optionId, Long questionId, Long catalogItemId,
            EffectType effect, Integer quantity) {
        if (catalogItemId == null)
            throw new IllegalArgumentException("catalogItemId is required");
        if (effect == null)
            throw new IllegalArgumentException("effect is required");
        boolean byOption = optionId != null;
        boolean byQuestion = questionId != null;
        if (byOption == byQuestion)
            throw new IllegalArgumentException(
                    "exactly one trigger is required: either optionId or questionId");
        if (effect == EffectType.SET_QUANTITY) {
            if (quantity == null || quantity <= 0)
                throw new IllegalArgumentException(
                        "SET_QUANTITY requires a quantity greater than 0");
        } else if (quantity != null) {
            throw new IllegalArgumentException("quantity is only allowed for SET_QUANTITY");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getOptionId() {
        return optionId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public EffectType getEffect() {
        return effect;
    }

    public Integer getQuantity() {
        return quantity;
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
