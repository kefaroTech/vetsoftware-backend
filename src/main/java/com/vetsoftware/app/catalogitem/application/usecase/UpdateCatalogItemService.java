package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.command.UpdateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.in.UpdateCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.update")
@Service
public class UpdateCatalogItemService implements UpdateCatalogItemUseCase {

    private final CatalogItemRepository repository;

    public UpdateCatalogItemService(CatalogItemRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CatalogItemDto execute(UpdateCatalogItemCommand command) {
        CatalogItem item = repository.findById(command.id())
                .orElseThrow(() -> new CatalogItemNotFoundException(command.id()));
        item.update(command.name(), command.shortDescription(), command.longDescription(),
                command.itemType(), command.capacityUnit(), command.core(), command.minQuantity(),
                command.maxQuantity(), command.sortOrder(), command.status());
        return CatalogItemDto.from(repository.save(item));
    }
}
