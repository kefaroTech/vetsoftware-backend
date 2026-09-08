package com.vetsoftware.app.companyusageevent.domain;

/**
 * El techo vigente de un eje, tal como lo resuelve la plataforma (excepción
 * negociada &gt; línea contratada &gt; escalón gratuito de fábrica &gt; sin
 * techo). Companion propio de esta rodaja: el resolutor real vive en
 * {@code companylimitoverride}, otra feature, y esta forma es lo único que
 * {@link com.vetsoftware.app.companyusageevent.application.port.out.EffectiveUsageLimitPort}
 * necesita devolver.
 */
public record EffectiveUsageLimit(Integer limitQuantity, boolean unlimited) {

    public EffectiveUsageLimit {
        if (!unlimited && limitQuantity == null) {
            throw new IllegalArgumentException(
                    "limitQuantity is required when the axis is not unlimited");
        }
    }
}
