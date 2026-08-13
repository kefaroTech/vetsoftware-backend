package com.vetsoftware.app.employee.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Sede asignada a un empleado, expuesta en el listado/detalle. */
public record BranchSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
