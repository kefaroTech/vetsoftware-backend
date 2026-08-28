package com.vetsoftware.app.companylimitoverride.application.usecase;

import com.vetsoftware.app.companylimitoverride.application.dto.EffectiveLimitDto;
import com.vetsoftware.app.companylimitoverride.application.port.in.ResolveEffectiveLimitUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.out.CompanyLimitOverrideRepository;
import com.vetsoftware.app.companylimitoverride.application.port.out.LimitCandidatesQueryPort;
import com.vetsoftware.app.companylimitoverride.domain.CompanyLimitOverride;
import com.vetsoftware.app.companylimitoverride.domain.EffectiveLimit;
import com.vetsoftware.app.companylimitoverride.domain.EffectiveLimitResolver;
import com.vetsoftware.app.companylimitoverride.domain.LimitCandidates;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el techo que rige y de dónde sale.
 *
 * <p>
 * <strong>Todo el criterio vive en {@code EffectiveLimitResolver}</strong>, que
 * es una función pura y probada sin base de datos. Este service solo hace lo
 * que el resolutor no puede hacer: recoger los candidatos. Esa separación es la
 * razón de que la precedencia se pueda cambiar en un solo sitio — y de que
 * cambiarla se vea en un diff de una clase.
 *
 * <p>
 * <strong>La excepción viva no basta: tiene que regir hoy.</strong> «Viva» son
 * dos condiciones de la columna generada —ni revocada ni con fecha de fin
 * escrita— y ninguna de las dos mira {@code validFrom}. Una excepción negociada
 * hoy para que empiece el mes que viene está viva y <em>todavía no manda</em>;
 * dársela por buena le subiría el techo al cliente antes de lo pactado, que es
 * un error que nadie reclama y que por eso no se descubre.
 *
 * <p>
 * <strong>Carga acotada por empresa y eje, nunca por id.</strong> El puerto de
 * salida no ofrece ninguna carga «por id» suelta a propósito: una excepción es
 * de alguien.
 */
@Service
public class ResolveEffectiveLimitService implements ResolveEffectiveLimitUseCase {

    private final CompanyLimitOverrideRepository repository;
    private final LimitCandidatesQueryPort candidatesQueryPort;
    private final Clock clock;

    public ResolveEffectiveLimitService(CompanyLimitOverrideRepository repository,
            LimitCandidatesQueryPort candidatesQueryPort, Clock clock) {
        this.repository = repository;
        this.candidatesQueryPort = candidatesQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public EffectiveLimitDto resolve(Long companyId, Long limitDimensionId) {
        LocalDate today = LocalDate.now(clock);
        Optional<CompanyLimitOverride> ruling = repository
                .findAliveByCompanyIdAndLimitDimensionId(companyId, limitDimensionId)
                .filter(override -> override.rulesOn(today));
        Integer overrideQuantity = ruling.map(CompanyLimitOverride::getLimitQuantity).orElse(null);
        Long overrideId = ruling.map(CompanyLimitOverride::getId).orElse(null);

        LimitCandidates candidates = candidatesQueryPort.findCandidates(companyId,
                limitDimensionId);
        EffectiveLimit limit = EffectiveLimitResolver.resolve(overrideQuantity, overrideId,
                candidates.contractedQuantities(), candidates.catalogDefaultQuantities(),
                candidates.axisPredatesContract());
        return EffectiveLimitDto.from(companyId, limitDimensionId, limit);
    }
}
