package com.vetsoftware.app.limitdimension.application.usecase;

import com.vetsoftware.app.limitdimension.application.command.CreateLimitDimensionCommand;
import com.vetsoftware.app.limitdimension.application.dto.LimitDimensionDto;
import com.vetsoftware.app.limitdimension.application.port.in.CreateLimitDimensionUseCase;
import com.vetsoftware.app.limitdimension.application.port.out.LimitDimensionRepository;
import com.vetsoftware.app.limitdimension.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.limitdimension.domain.LimitDimension;
import com.vetsoftware.app.limitdimension.domain.LimitDimensionCodeAlreadyExistsException;
import com.vetsoftware.app.limitdimension.domain.SubModuleRef;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Declara un eje limitable.
 *
 * <p>
 * El submódulo se resuelve por puerto y no se confía: apuntar a uno inexistente
 * moriría en la clave foránea a mitad de transacción, y un error del motor no
 * le dice a nadie qué corregir.
 */
@Service
public class CreateLimitDimensionService implements CreateLimitDimensionUseCase {

    private final LimitDimensionRepository repository;
    private final SubModuleQueryPort subModuleQueryPort;
    private final Clock clock;

    public CreateLimitDimensionService(LimitDimensionRepository repository,
            SubModuleQueryPort subModuleQueryPort, Clock clock) {
        this.repository = repository;
        this.subModuleQueryPort = subModuleQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public LimitDimensionDto execute(CreateLimitDimensionCommand command) {
        if (repository.existsByCode(command.code()))
            throw new LimitDimensionCodeAlreadyExistsException(command.code());
        SubModuleRef subModule = command.subModuleId() == null
                ? null
                : subModuleQueryPort.findById(command.subModuleId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Sub module " + command.subModuleId() + " not found"));
        LimitDimension dimension = LimitDimension.create(command.code(), command.name(),
                command.measureKind(), subModule, command.releaseDelayDays(),
                command.availableFrom(), LocalDateTime.now(clock));
        return LimitDimensionDto.from(repository.save(dimension));
    }
}
