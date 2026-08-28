package com.vetsoftware.app.limitdimension.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * El submódulo del que cuelga un eje, tal como sale por HTTP.
 *
 * <p>
 * Companion local: esta feature <strong>no</strong> importa el
 * {@code SubModuleResponse} de la feature {@code submodule} —lo prohíbe el
 * vertical slicing— y tampoco publica el {@code SubModuleRef} del dominio.
 *
 * <p>
 * <strong>Por qué lleva nombre propio en el contrato.</strong> Ya hay cuatro
 * features con un {@code SubModuleSummary} en {@code web/response}
 * —{@code basepermission}, {@code catalogitem}, {@code entitlement} y
 * {@code permission}— y springdoc las funde en <em>un solo</em> esquema
 * {@code SubModuleSummary}, hoy con los tres campos obligatorios. Este record
 * no puede declarar {@code name} obligatorio, porque {@code SubModuleRef} de
 * esta feature solo exige {@code id} y {@code code}; si entrara en esa fusión y
 * ganara por orden de escaneo, {@code name} dejaría de ser obligatorio <em>para
 * las cuatro features existentes</em> y los dos frontends verían cambiar un
 * tipo compartido sin que nadie hubiera tocado su feature. El
 * {@code @Schema(name = ...)} lo saca de la colisión: lo que se publica es un
 * esquema aparte y ningún consumidor actual se entera.
 */
@Schema(name = "LimitDimensionSubModuleSummary")
public record SubModuleSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code, String name) {
}
