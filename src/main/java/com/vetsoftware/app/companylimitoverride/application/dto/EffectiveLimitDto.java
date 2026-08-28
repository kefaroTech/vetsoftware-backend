package com.vetsoftware.app.companylimitoverride.application.dto;

import com.vetsoftware.app.companylimitoverride.domain.EffectiveLimit;
import com.vetsoftware.app.companylimitoverride.domain.LimitSource;

/**
 * El techo que rige de verdad sobre un eje, con la etiqueta de de dónde salió.
 *
 * <p>
 * <strong>{@code limitQuantity} vacío es «sin techo», que no es lo mismo que
 * cero.</strong> Cero es un techo real que no deja crear nada. Por eso viaja
 * también {@code unlimited} calculado: dejar esa distinción a cada pantalla es
 * como se acaba pintando «0 de 0» donde la verdad es «este límite no aplica».
 *
 * @param overrideId
 *            la excepción negociada de la que salió el techo, cuando el origen
 *            es {@code COMPANY_OVERRIDE}. Es lo que permite abrir el papel de
 *            la decisión desde la pantalla de cupos
 */
public record EffectiveLimitDto(Long companyId, Long limitDimensionId, Integer limitQuantity,
        LimitSource source, Long overrideId, boolean unlimited) {

    public static EffectiveLimitDto from(Long companyId, Long limitDimensionId,
            EffectiveLimit limit) {
        return new EffectiveLimitDto(companyId, limitDimensionId, limit.limitQuantity(),
                limit.source(), limit.overrideId(), limit.isUnlimited());
    }
}
