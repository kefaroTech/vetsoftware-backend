package com.vetsoftware.app.companyusageevent.application.command;

/**
 * Comprobar si la empresa todavía tiene margen en un eje contable, antes de
 * crear el registro que lo consumiría.
 *
 * @param periodKey
 *            {@code ALLTIME} para un eje acumulativo, o el mes en curso
 *            ({@code AAAA-MM}) para un eje de flujo. Lo decide el llamante: es
 *            propiedad del eje (cuenta desde siempre o se reinicia cada mes),
 *            no algo que este comando pueda derivar
 */
public record CheckUsageLimitCommand(Long companyId, String limitDimensionCode, String periodKey) {
}
