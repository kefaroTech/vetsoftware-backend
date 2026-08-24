package com.vetsoftware.app.configurator.application.dto;

import com.vetsoftware.app.configurator.domain.ConfiguratorOption;

/**
 * Una opción tal como la ve un prospecto sin autenticar: lo justo para
 * pintarla.
 *
 * <p>
 * Deliberadamente más pobre que {@link ConfiguratorOptionDto}: no lleva
 * {@code createdDate} ni {@code enabled}, que son datos de administración. El
 * cuestionario público solo devuelve filas activas, así que {@code enabled}
 * sería siempre {@code true} — un literal booleano proyectado que no informa de
 * nada y que el front acabaría interpretando.
 */
public record QuestionnaireOptionDto(Long id, String code, String label, String helpText,
        int sortOrder) {

    public static QuestionnaireOptionDto from(ConfiguratorOption option) {
        return new QuestionnaireOptionDto(option.getId(), option.getCode(), option.getLabel(),
                option.getHelpText(), option.getSortOrder());
    }
}
