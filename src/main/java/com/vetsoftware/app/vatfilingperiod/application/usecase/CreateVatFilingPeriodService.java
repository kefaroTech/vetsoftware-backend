package com.vetsoftware.app.vatfilingperiod.application.usecase;

import com.vetsoftware.app.vatfilingperiod.application.command.CreateVatFilingPeriodCommand;
import com.vetsoftware.app.vatfilingperiod.application.dto.VatFilingPeriodDto;
import com.vetsoftware.app.vatfilingperiod.application.port.in.CreateVatFilingPeriodUseCase;
import com.vetsoftware.app.vatfilingperiod.application.port.out.VatFilingPeriodRepository;
import com.vetsoftware.app.vatfilingperiod.domain.VatFilingPeriod;
import com.vetsoftware.app.vatfilingperiod.domain.VatFilingPeriodAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vatfiling.create")
@Service
public class CreateVatFilingPeriodService implements CreateVatFilingPeriodUseCase {

    private final VatFilingPeriodRepository repository;
    private final Clock clock;

    public CreateVatFilingPeriodService(VatFilingPeriodRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public VatFilingPeriodDto execute(CreateVatFilingPeriodCommand command) {
        if (repository.existsByFiscalYear(command.fiscalYear())) {
            throw new VatFilingPeriodAlreadyExistsException(command.fiscalYear());
        }
        VatFilingPeriod period = VatFilingPeriod.create(command.fiscalYear(), command.frequency(),
                command.legalReference(), LocalDateTime.now(clock));
        return VatFilingPeriodDto.from(repository.save(period));
    }
}
