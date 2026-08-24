package com.vetsoftware.app.configurator.domain;

/**
 * Las dos claves únicas del efecto —{@code uq_configurator_effects_option} y
 * {@code uq_configurator_effects_question}— impiden que el mismo disparador
 * meta dos veces el mismo artículo con el mismo tipo de efecto. Sin una
 * comprobación previa el choque lo detecta la base, y lo que sale por HTTP es
 * un 409 con el detalle genérico {@code Database constraint violation} sobre
 * una fila que el administrador no puede ver ni listar.
 *
 * <p>
 * El efecto no tiene código de negocio, así que el mensaje nombra la terna
 * entera: es lo único que permite encontrar la fila que estorba.
 */
public class ConfiguratorEffectAlreadyExistsException extends RuntimeException {
    public ConfiguratorEffectAlreadyExistsException(Long optionId, Long questionId,
            Long catalogItemId, EffectType effect) {
        super("ConfiguratorEffect already exists: trigger="
                + (optionId != null ? "option " + optionId : "question " + questionId)
                + ", catalogItemId=" + catalogItemId + ", effect=" + effect);
    }
}
