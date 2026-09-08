package com.vetsoftware.app.companyusageevent.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companylimitoverride.application.dto.EffectiveLimitDto;
import com.vetsoftware.app.companylimitoverride.application.port.in.ResolveEffectiveLimitUseCase;
import com.vetsoftware.app.companyusageevent.application.port.out.EffectiveUsageLimitPort;
import com.vetsoftware.app.companyusageevent.domain.EffectiveUsageLimit;
import org.springframework.stereotype.Component;

/**
 * El único archivo de esta rodaja que conoce {@code companylimitoverride}.
 *
 * <p>
 * <strong>Corre como {@code SYSTEM}</strong>, no como el empleado que está
 * creando el recurso: {@code ResolveEffectiveLimitUseCase} exige la autoridad
 * {@code companyLimitOverride.read}, que ningún rol operativo siembra (es de la
 * pantalla de cupos, no de crear una mascota). Resolver el techo vigente para
 * decidir si se bloquea la creación es un efecto del sistema, no una consulta
 * que el usuario pida — mismo razonamiento que ya usan los adaptadores de
 * {@code registration} para llamar puertos cerrados a plataforma.
 */
@Component
public class ResolveEffectiveUsageLimitAdapter implements EffectiveUsageLimitPort {

    private final ResolveEffectiveLimitUseCase resolveEffectiveLimit;
    private final SystemAuthRunner systemAuthRunner;

    public ResolveEffectiveUsageLimitAdapter(ResolveEffectiveLimitUseCase resolveEffectiveLimit,
            SystemAuthRunner systemAuthRunner) {
        this.resolveEffectiveLimit = resolveEffectiveLimit;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public EffectiveUsageLimit resolve(Long companyId, Long limitDimensionId) {
        EffectiveLimitDto limit = systemAuthRunner
                .call(() -> resolveEffectiveLimit.resolve(companyId, limitDimensionId));
        return new EffectiveUsageLimit(limit.limitQuantity(), limit.unlimited());
    }
}
