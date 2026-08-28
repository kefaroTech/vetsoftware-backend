package com.vetsoftware.app.platformtaxprofile.infrastructure.web.response;

import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformEconomicActivitySummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * La actividad economica dentro de la identidad fiscal de la plataforma.
 *
 * <p>
 * <strong>Companion propio y no la {@code EconomicActivitySummary} de
 * {@code companytaxprofile}</strong>: un {@code web/response} nunca importa el
 * de otra feature.
 *
 * <p>
 * <strong>El nombre lleva el prefijo {@code Platform} a proposito, aunque hoy
 * la forma sea identica.</strong> springdoc nombra los esquemas por el
 * <em>nombre simple</em>, asi que un record llamado
 * {@code EconomicActivitySummary} colapsaria con el que ya publica
 * {@code companytaxprofile} en un unico
 * {@code #/components/schemas/EconomicActivitySummary}. Con la forma
 * coincidente eso seria correcto —es lo que ya hacen los cuatro
 * {@code CitySummary} del arbol—, pero dejaria a esta rodaja capaz de cambiar,
 * añadiendo un campo, el esquema del que dependen los tipos generados de los
 * dos fronts para la ficha fiscal de las clinicas. Un prefijo aqui cuesta un
 * esquema mas en el contrato y compra que las dos rodajas no puedan pisarse.
 */
public record PlatformEconomicActivitySummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {

    /** {@code null} entra y {@code null} sale: la actividad es opcional. */
    public static PlatformEconomicActivitySummary from(PlatformEconomicActivitySummaryDto dto) {
        return dto == null
                ? null
                : new PlatformEconomicActivitySummary(dto.id(), dto.code(), dto.name());
    }
}
