package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
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
    private final Clock clock;

    public CreateCatalogItemService(CatalogItemRepository repository, Clock clock) {
        this.repository = repository;
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
        return CatalogItemDto.from(repository.save(item));
    }
}
