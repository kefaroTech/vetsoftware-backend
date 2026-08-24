package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.command.CreateBundleComponentCommand;
import com.vetsoftware.app.catalogitem.application.dto.BundleComponentDto;
import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateBundleComponentUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.BundleComponentRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.BundleComponent;
import com.vetsoftware.app.catalogitem.domain.BundleComponentAlreadyExistsException;
import com.vetsoftware.app.catalogitem.domain.BundleComponentNotFoundException;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.domain.InvalidBundleCompositionException;
import com.vetsoftware.app.catalogitem.domain.LinkOutcome;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta de una pieza dentro de un paquete, con las dos comprobaciones que la
 * ficha 4 baja explícitamente a las reglas de código: un {@code CHECK} de MySQL
 * no puede leer {@code catalog_items.item_type} porque el manual le prohíbe las
 * «columns in other tables».
 */
@Observed(name = "catalogitem.bundlecomponent.create")
@Service
public class CreateBundleComponentService implements CreateBundleComponentUseCase {

    private final BundleComponentRepository repository;
    private final CatalogItemRepository catalogItemRepository;
    private final Clock clock;

    public CreateBundleComponentService(BundleComponentRepository repository,
            CatalogItemRepository catalogItemRepository, Clock clock) {
        this.repository = repository;
        this.catalogItemRepository = catalogItemRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BundleComponentDto execute(CreateBundleComponentCommand command) {
        CatalogItem bundle = load(command.bundleItemId());
        CatalogItem component = load(command.componentItemId());

        if (!bundle.isBundle()) {
            throw new InvalidBundleCompositionException(
                    "Catalog item " + bundle.getId() + " cannot hold components: its item type is "
                            + bundle.getItemType() + " and only BUNDLE can");
        }
        if (component.isBundle()) {
            throw new InvalidBundleCompositionException("Nested bundles are not allowed: component "
                    + component.getId() + " is itself a BUNDLE");
        }

        BundleComponent domainComponent = BundleComponent.create(bundle.getId(), component.getId(),
                command.quantity(), clock);

        Optional<LinkStateDto> existing = repository.findAnyByPair(bundle.getId(),
                component.getId());
        if (existing.isPresent()) {
            LinkStateDto state = existing.get();
            if (state.enabled()) {
                throw new BundleComponentAlreadyExistsException(bundle.getId(), component.getId());
            }
            repository.reactivate(state.id());
            BundleComponent revived = repository.findById(state.id())
                    .orElseThrow(() -> new BundleComponentNotFoundException(state.id()));
            revived.changeQuantity(command.quantity());
            return BundleComponentDto.from(repository.save(revived), LinkOutcome.REACTIVATED);
        }
        return BundleComponentDto.from(repository.save(domainComponent), LinkOutcome.CREATED);
    }

    private CatalogItem load(Long id) {
        return catalogItemRepository.findById(id)
                .orElseThrow(() -> new CatalogItemNotFoundException(id));
    }
}
