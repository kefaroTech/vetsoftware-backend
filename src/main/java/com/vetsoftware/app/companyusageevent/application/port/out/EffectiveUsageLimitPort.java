package com.vetsoftware.app.companyusageevent.application.port.out;

import com.vetsoftware.app.companyusageevent.domain.EffectiveUsageLimit;

/**
 * El techo vigente de un eje, resuelto por la plataforma. El único archivo de
 * esta rodaja que conoce {@code companylimitoverride} es el adaptador de
 * {@code infrastructure/orchestration} que implementa este puerto.
 */
public interface EffectiveUsageLimitPort {

    EffectiveUsageLimit resolve(Long companyId, Long limitDimensionId);
}
