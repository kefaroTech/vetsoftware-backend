package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemDependencyCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDependencyDto;
import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateCatalogItemDependencyUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemDependencyRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependency;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyAlreadyExistsException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyCycleException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyNotFoundException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.domain.DependencyGraph;
import com.vetsoftware.app.catalogitem.domain.LinkOutcome;
import com.vetsoftware.app.catalogitem.domain.RelationType;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta de una regla del configurador, y el sitio donde vive la <strong>regla
 * R16</strong>: las dependencias entre artículos no pueden formar ciclos
 * indirectos.
 */
@Observed(name = "catalogitem.dependency.create")
@Service
public class CreateCatalogItemDependencyService implements CreateCatalogItemDependencyUseCase {

    private final CatalogItemDependencyRepository repository;
    private final CatalogItemRepository catalogItemRepository;
    private final Clock clock;

    public CreateCatalogItemDependencyService(CatalogItemDependencyRepository repository,
            CatalogItemRepository catalogItemRepository, Clock clock) {
        this.repository = repository;
        this.catalogItemRepository = catalogItemRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CatalogItemDependencyDto execute(CreateCatalogItemDependencyCommand command) {
        CatalogItem subject = load(command.catalogItemId());
        CatalogItem related = load(command.relatedItemId());

        // Construir primero: el dominio rechaza el arco a sí mismo y la nota
        // demasiado larga, y esos son 400, no 409. Comprobar el ciclo antes daría
        // el código de error equivocado para el ciclo trivial.
        CatalogItemDependency dependency = CatalogItemDependency.create(subject.getId(),
                related.getId(), command.relationType(), command.note(), clock);

        // Antes de la rama de duplicados a propósito: reactivar un arco REQUIRES
        // desactivado lo devuelve al grafo igual que insertarlo, y el grafo que se
        // carga solo trae los arcos activos, así que el ciclo que cerraría no se
        // vería en la comprobación.
        rejectIfClosesCycle(dependency.getRelationType(), subject.getId(), related.getId());

        Optional<LinkStateDto> existing = repository.findAnyByTriple(subject.getId(),
                related.getId(), command.relationType());
        if (existing.isPresent()) {
            return reviveOrReject(existing.get(), command);
        }
        return CatalogItemDependencyDto.from(repository.save(dependency), LinkOutcome.CREATED);
    }

    private CatalogItemDependencyDto reviveOrReject(LinkStateDto state,
            CreateCatalogItemDependencyCommand command) {
        if (state.enabled()) {
            throw new CatalogItemDependencyAlreadyExistsException(command.catalogItemId(),
                    command.relatedItemId(), command.relationType());
        }
        repository.reactivate(state.id());
        CatalogItemDependency revived = repository.findById(state.id())
                .orElseThrow(() -> new CatalogItemDependencyNotFoundException(state.id()));
        revived.update(command.relationType(), command.note());
        return CatalogItemDependencyDto.from(repository.save(revived), LinkOutcome.REACTIVATED);
    }

    /**
     * Solo los arcos {@code REQUIRES} arrastran, así que solo ellos pueden ciclar.
     * Un ciclo de {@code RECOMMENDS} es inofensivo y {@code EXCLUDES} no encadena
     * nada — el mismo criterio que usa la consulta de vigilancia recursiva de R16,
     * para que las dos digan lo mismo.
     */
    private void rejectIfClosesCycle(RelationType relationType, Long catalogItemId,
            Long relatedItemId) {
        if (relationType != RelationType.REQUIRES) {
            return;
        }
        DependencyGraph graph = DependencyGraph.ofRequires(repository.findAllRequiresEdges());
        List<Long> cycle = graph.cycleClosedBy(catalogItemId, relatedItemId);
        if (!cycle.isEmpty()) {
            throw new CatalogItemDependencyCycleException(cycle);
        }
    }

    private CatalogItem load(Long id) {
        return catalogItemRepository.findById(id)
                .orElseThrow(() -> new CatalogItemNotFoundException(id));
    }
}
