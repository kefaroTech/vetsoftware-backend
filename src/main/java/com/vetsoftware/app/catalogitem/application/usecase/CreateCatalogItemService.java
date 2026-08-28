package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemCodeAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.create")
@Service
public class CreateCatalogItemService implements CreateCatalogItemUseCase {

    private final CatalogItemRepository repository;
    private final LimitDimensionQueryPort limitDimensionQueryPort;
    private final Clock clock;

    public CreateCatalogItemService(CatalogItemRepository repository,
            LimitDimensionQueryPort limitDimensionQueryPort, Clock clock) {
        this.repository = repository;
        this.limitDimensionQueryPort = limitDimensionQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CatalogItemDto execute(CreateCatalogItemCommand command) {
        if (repository.existsByCodeIgnoringEnabled(command.code())) {
            throw new CatalogItemCodeAlreadyExistsException(command.code());
        }
        CatalogItem item = CatalogItem.create(command.code(), command.name(),
                command.shortDescription(), command.longDescription(), command.itemType(),
                command.capacityUnit(), command.core(), command.minQuantity(),
                command.maxQuantity(), command.sortOrder(), command.status(), clock);
        // Despues de construir y no antes: el dominio decide primero si el articulo
        // puede llevar unidad -un MODULE con unidad se rechaza por lo que es, no por
        // el eje que nombra- y solo entonces tiene sentido preguntarle al catalogo si
        // ese eje existe. Al reves, un MODULE con unidad recibiria el mensaje
        // equivocado y ademas gastaria una consulta para nada.
        CapacityUnitCatalogGuard.requireKnownAxis(limitDimensionQueryPort, item.getCapacityUnit());
        return CatalogItemDto.from(repository.save(item));
    }
}
