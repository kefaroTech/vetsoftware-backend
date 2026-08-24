package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemSubModuleCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemSubModuleDto;
import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateCatalogItemSubModuleUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemSubModuleRepository;
import com.vetsoftware.app.catalogitem.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModule;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModuleAlreadyExistsException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModuleNotFoundException;
import com.vetsoftware.app.catalogitem.domain.LinkOutcome;
import com.vetsoftware.app.catalogitem.domain.SubModuleRef;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.submodule.create")
@Service
public class CreateCatalogItemSubModuleService implements CreateCatalogItemSubModuleUseCase {

    private final CatalogItemSubModuleRepository repository;
    private final CatalogItemRepository catalogItemRepository;
    private final SubModuleQueryPort subModuleQueryPort;
    private final Clock clock;

    public CreateCatalogItemSubModuleService(CatalogItemSubModuleRepository repository,
            CatalogItemRepository catalogItemRepository, SubModuleQueryPort subModuleQueryPort,
            Clock clock) {
        this.repository = repository;
        this.catalogItemRepository = catalogItemRepository;
        this.subModuleQueryPort = subModuleQueryPort;
        this.clock = clock;
    }

    /**
     * Reactiva el vínculo en vez de insertar otro cuando el par ya existe
     * desactivado. Ver {@link LinkStateDto}: la fila dada de baja sigue ocupando
     * {@code uq_catalog_item_sub_modules} aunque el {@code @SQLRestriction} la
     * esconda, así que un {@code INSERT} chocaría contra algo invisible.
     */
    @Override
    @Transactional
    public CatalogItemSubModuleDto execute(CreateCatalogItemSubModuleCommand command) {
        CatalogItem item = catalogItemRepository.findById(command.catalogItemId())
                .orElseThrow(() -> new CatalogItemNotFoundException(command.catalogItemId()));
        SubModuleRef subModule = subModuleQueryPort.findById(command.subModuleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "SubModule not found: " + command.subModuleId()));

        Optional<LinkStateDto> existing = repository.findAnyByPair(item.getId(), subModule.id());
        if (existing.isPresent()) {
            LinkStateDto state = existing.get();
            if (state.enabled()) {
                throw new CatalogItemSubModuleAlreadyExistsException(item.getId(), subModule.id());
            }
            repository.reactivate(state.id());
            return CatalogItemSubModuleDto.from(
                    repository.findById(state.id()).orElseThrow(
                            () -> new CatalogItemSubModuleNotFoundException(state.id())),
                    LinkOutcome.REACTIVATED);
        }

        CatalogItemSubModule link = CatalogItemSubModule.create(item.getId(), subModule, clock);
        return CatalogItemSubModuleDto.from(repository.save(link), LinkOutcome.CREATED);
    }
}
