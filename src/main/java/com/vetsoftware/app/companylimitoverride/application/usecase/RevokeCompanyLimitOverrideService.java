package com.vetsoftware.app.companylimitoverride.application.usecase;

import com.vetsoftware.app.companylimitoverride.application.command.RevokeCompanyLimitOverrideCommand;
import com.vetsoftware.app.companylimitoverride.application.dto.CompanyLimitOverrideDto;
import com.vetsoftware.app.companylimitoverride.application.port.in.RevokeCompanyLimitOverrideUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.out.CompanyLimitOverrideRepository;
import com.vetsoftware.app.companylimitoverride.domain.CompanyLimitOverride;
import com.vetsoftware.app.companylimitoverride.domain.CompanyLimitOverrideNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cierra una excepción negociada, dejando la decisión auditada. */
@Service
public class RevokeCompanyLimitOverrideService implements RevokeCompanyLimitOverrideUseCase {

    private final CompanyLimitOverrideRepository repository;
    private final Clock clock;

    public RevokeCompanyLimitOverrideService(CompanyLimitOverrideRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyLimitOverrideDto execute(RevokeCompanyLimitOverrideCommand command) {
        CompanyLimitOverride override = repository
                .findAliveByCompanyIdAndLimitDimensionId(command.companyId(),
                        command.limitDimensionId())
                .orElseThrow(() -> new CompanyLimitOverrideNotFoundException(command.companyId(),
                        command.limitDimensionId()));
        return CompanyLimitOverrideDto.from(repository
                .save(override.revoke(LocalDateTime.now(clock), command.revokedBySystemUserId(),
                        command.revokedReasonCode(), command.revokedReason())));
    }
}
