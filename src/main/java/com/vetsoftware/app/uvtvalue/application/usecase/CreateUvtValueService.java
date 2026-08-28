package com.vetsoftware.app.uvtvalue.application.usecase;

import com.vetsoftware.app.uvtvalue.application.command.CreateUvtValueCommand;
import com.vetsoftware.app.uvtvalue.application.dto.UvtValueDto;
import com.vetsoftware.app.uvtvalue.application.port.in.CreateUvtValueUseCase;
import com.vetsoftware.app.uvtvalue.application.port.out.UvtValueRepository;
import com.vetsoftware.app.uvtvalue.domain.UvtValue;
import com.vetsoftware.app.uvtvalue.domain.UvtValueAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "uvt.create")
@Service
public class CreateUvtValueService implements CreateUvtValueUseCase {

    private final UvtValueRepository repository;
    private final Clock clock;

    public CreateUvtValueService(UvtValueRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UvtValueDto execute(CreateUvtValueCommand command) {
        if (repository.existsByFiscalYear(command.fiscalYear())) {
            throw new UvtValueAlreadyExistsException(command.fiscalYear());
        }
        UvtValue value = UvtValue.create(command.fiscalYear(), command.valueAmount(),
                command.legalReference(), LocalDateTime.now(clock));
        return UvtValueDto.from(repository.save(value));
    }
}
