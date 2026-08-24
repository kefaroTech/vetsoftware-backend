package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.dto.BundleComponentDto;
import com.vetsoftware.app.catalogitem.application.port.in.ListBundleComponentsUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.BundleComponentRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.bundlecomponent.list")
@Service
public class ListBundleComponentsService implements ListBundleComponentsUseCase {

    private final BundleComponentRepository repository;
    private final CatalogItemRepository catalogItemRepository;

    public ListBundleComponentsService(BundleComponentRepository repository,
            CatalogItemRepository catalogItemRepository) {
        this.repository = repository;
        this.catalogItemRepository = catalogItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BundleComponentDto> listByBundle(Long bundleItemId) {
        catalogItemRepository.findById(bundleItemId)
                .orElseThrow(() -> new CatalogItemNotFoundException(bundleItemId));
        return repository.findAllByBundleItemId(bundleItemId).stream().map(BundleComponentDto::from)
                .toList();
    }
}
