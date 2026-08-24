package com.vetsoftware.app.catalogitem.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Companion local del submódulo. <strong>No</strong> se reutiliza el
 * {@code SubModuleResponse} de la otra feature: el {@code CLAUDE.md} lo prohíbe
 * explícitamente, y por una razón práctica —el día que aquella feature añada un
 * campo a su respuesta, el contrato de esta cambiaría sin que nadie lo hubiera
 * decidido aquí.
 */
public record SubModuleSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code) {
}
