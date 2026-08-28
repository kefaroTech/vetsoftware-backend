package com.vetsoftware.app.companylimitoverride.application.usecase;

import com.vetsoftware.app.companylimitoverride.application.command.GrantCompanyLimitOverrideCommand;
import com.vetsoftware.app.companylimitoverride.application.dto.CompanyLimitOverrideDto;
import com.vetsoftware.app.companylimitoverride.application.port.in.GrantCompanyLimitOverrideUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.out.CompanyLimitOverrideRepository;
import com.vetsoftware.app.companylimitoverride.domain.CompanyAlreadyHasLimitOverrideException;
import com.vetsoftware.app.companylimitoverride.domain.CompanyLimitOverride;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Negocia una excepción de techo.
 *
 * <p>
 * Comprueba antes que no haya otra viva <em>sobre el mismo eje</em> —no sobre
 * la empresa entera—: negociar 300 mascotas y 5 usuarios en la misma llamada
 * son dos excepciones legítimas y las dos tienen que entrar.
 */
@Service
public class GrantCompanyLimitOverrideService implements GrantCompanyLimitOverrideUseCase {

    private final CompanyLimitOverrideRepository repository;
    private final Clock clock;

    public GrantCompanyLimitOverrideService(CompanyLimitOverrideRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyLimitOverrideDto execute(GrantCompanyLimitOverrideCommand command) {
        if (repository.existsAliveByCompanyIdAndLimitDimensionId(command.companyId(),
                command.limitDimensionId()))
            throw new CompanyAlreadyHasLimitOverrideException(command.companyId(),
                    command.limitDimensionId());
        CompanyLimitOverride override = CompanyLimitOverride.grant(command.companyId(),
                command.limitDimensionId(), command.limitQuantity(), command.validFrom(),
                command.reasonCode(), command.reason(), command.grantedBySystemUserId(),
                LocalDateTime.now(clock));
        return CompanyLimitOverrideDto.from(repository.save(override));
    }
}
