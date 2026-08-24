package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.in.FindCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "catalogitem.find")
@Service
public class FindCatalogItemService implements FindCatalogItemUseCase {

    private final CatalogItemRepository repository;

    public FindCatalogItemService(CatalogItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public CatalogItemDto findById(Long id) {
        return CatalogItemDto.from(
                repository.findById(id).orElseThrow(() -> new CatalogItemNotFoundException(id)));
    }
}
