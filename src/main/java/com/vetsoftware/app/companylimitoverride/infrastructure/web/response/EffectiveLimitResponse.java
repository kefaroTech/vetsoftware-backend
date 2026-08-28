package com.vetsoftware.app.companylimitoverride.infrastructure.web.response;

import com.vetsoftware.app.companylimitoverride.application.dto.EffectiveLimitDto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * El techo que rige, con su procedencia — lo que la pantalla de cupos necesita
 * para explicar el número que pinta.
 *
 * <p>
 * <strong>{@code source} es la mitad del valor de esta respuesta.</strong> Un
 * cupo distinto del de catálogo sin decir de dónde sale es un número
 * inexplicable, y la pregunta que llega a soporte es siempre la misma: «¿por
 * qué tengo 300 y no 100?». Con el origen dentro se responde mirando una fila,
 * y cuando es {@code COMPANY_OVERRIDE} el {@code overrideId} abre el papel de
 * la decisión que lo pactó.
 *
 * <p>
 * <strong>Viaja como texto con su lista cerrada anotada, no como tipo con
 * nombre propio.</strong> Hay dos enumerados {@code LimitSource} en el árbol
 * —el de esta rodaja y el de {@code companylimitevent}— y springdoc funde por
 * nombre simple: exponerlos como tipo dejaría en el contrato un único esquema
 * {@code LimitSource} que hoy cuadra <em>por casualidad</em>, porque sus cuatro
 * valores coinciden. Con {@code allowableValues} los dos fronts reciben la
 * lista igual de cerrada y sin esquema compartido que se rompa el día que uno
 * de los dos gane un valor.
 *
 * <p>
 * <strong>{@code limitQuantity} vacío es «sin techo», no cero</strong>, y por
 * eso {@code unlimited} viaja calculado: cero es un techo real que no deja
 * crear nada, y confundirlos es la diferencia entre una clínica bloqueada y una
 * clínica sin límite.
 */
public record EffectiveLimitResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long limitDimensionId,
        @Schema(description = "Vacío significa sin techo, que no es lo mismo que cero") Integer limitQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "De dónde sale el techo, en orden de precedencia descendente", allowableValues = {
                "COMPANY_OVERRIDE", "SUBSCRIPTION", "CATALOG_DEFAULT", "NONE"}) String source,
        @Schema(description = "La excepción negociada de la que sale, solo con origen"
                + " COMPANY_OVERRIDE") Long overrideId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean unlimited) {

    public static EffectiveLimitResponse from(EffectiveLimitDto dto) {
        return new EffectiveLimitResponse(dto.companyId(), dto.limitDimensionId(),
                dto.limitQuantity(), dto.source().name(), dto.overrideId(), dto.unlimited());
    }
}
