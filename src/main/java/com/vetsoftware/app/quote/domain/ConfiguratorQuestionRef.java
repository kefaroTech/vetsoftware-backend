package com.vetsoftware.app.quote.domain;

/**
 * Companion VO de la pregunta del configurador, leido solo para copiar su
 * codigo en {@link QuoteAnswer}: el cuestionario se reescribe y la respuesta
 * tiene que seguir siendo legible seis meses despues.
 */
public record ConfiguratorQuestionRef(Long id, String code) {
    public ConfiguratorQuestionRef {
        if (id == null)
            throw new IllegalArgumentException("configurator question id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("configurator question code is required");
    }
}
