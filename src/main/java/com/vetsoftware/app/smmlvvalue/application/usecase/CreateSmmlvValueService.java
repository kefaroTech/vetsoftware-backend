package com.vetsoftware.app.smmlvvalue.application.usecase;

import com.vetsoftware.app.smmlvvalue.application.command.CreateSmmlvValueCommand;
import com.vetsoftware.app.smmlvvalue.application.dto.SmmlvValueDto;
import com.vetsoftware.app.smmlvvalue.application.port.in.CreateSmmlvValueUseCase;
import com.vetsoftware.app.smmlvvalue.application.port.out.SmmlvValueRepository;
import com.vetsoftware.app.smmlvvalue.domain.SmmlvValue;
import com.vetsoftware.app.smmlvvalue.domain.SmmlvValueAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "smmlv.create")
@Service
public class CreateSmmlvValueService implements CreateSmmlvValueUseCase {

    private final SmmlvValueRepository repository;
    private final Clock clock;

    public CreateSmmlvValueService(SmmlvValueRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SmmlvValueDto execute(CreateSmmlvValueCommand command) {
        if (repository.existsByFiscalYear(command.fiscalYear())) {
            throw new SmmlvValueAlreadyExistsException(command.fiscalYear());
        }
        SmmlvValue value = SmmlvValue.create(command.fiscalYear(), command.valueAmount(),
                command.legalReference(), LocalDateTime.now(clock));
        return SmmlvValueDto.from(repository.save(value));
    }
}
