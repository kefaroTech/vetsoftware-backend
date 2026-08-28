package com.vetsoftware.app.catalogitemlimit.application.usecase;

import com.vetsoftware.app.catalogitemlimit.application.command.CreateCatalogItemLimitCommand;
import com.vetsoftware.app.catalogitemlimit.application.dto.CatalogItemLimitDto;
import com.vetsoftware.app.catalogitemlimit.application.port.in.CreateCatalogItemLimitUseCase;
import com.vetsoftware.app.catalogitemlimit.application.port.out.CatalogItemLimitRepository;
import com.vetsoftware.app.catalogitemlimit.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimit;
import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimitAlreadyExistsException;
import com.vetsoftware.app.catalogitemlimit.domain.LimitDimensionRef;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Declara el techo de fábrica de un artículo.
 *
 * <p>
 * El tipo de medida se lee del eje y no se acepta de fuera: es lo que permite
 * que el dominio compruebe, antes de llegar al motor, que un excedente no se
 * declara sobre un contador acumulativo.
 */
@Service
public class CreateCatalogItemLimitService implements CreateCatalogItemLimitUseCase {

    private final CatalogItemLimitRepository repository;
    private final LimitDimensionQueryPort limitDimensionQueryPort;
    private final Clock clock;

    public CreateCatalogItemLimitService(CatalogItemLimitRepository repository,
            LimitDimensionQueryPort limitDimensionQueryPort, Clock clock) {
        this.repository = repository;
        this.limitDimensionQueryPort = limitDimensionQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CatalogItemLimitDto execute(CreateCatalogItemLimitCommand command) {
        if (repository.existsByCatalogItemIdAndLimitDimensionId(command.catalogItemId(),
                command.limitDimensionId()))
            throw new CatalogItemLimitAlreadyExistsException(command.catalogItemId(),
                    command.limitDimensionId());
        LimitDimensionRef dimension = limitDimensionQueryPort.findById(command.limitDimensionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Limit dimension " + command.limitDimensionId() + " not found"));
        CatalogItemLimit limit = CatalogItemLimit.create(command.catalogItemId(), dimension.id(),
                dimension.measureKind(), command.mode(), command.limitQuantity(),
                command.resetPeriod(), command.enforcement(), command.overageUnitAmount(),
                command.warnThreshold(), command.trialMode(), command.trialLimitQuantity(),
                LocalDateTime.now(clock));
        return CatalogItemLimitDto.from(repository.save(limit));
    }
}
