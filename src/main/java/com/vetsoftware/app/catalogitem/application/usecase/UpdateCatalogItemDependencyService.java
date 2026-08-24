package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.command.UpdateCatalogItemDependencyCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDependencyDto;
import com.vetsoftware.app.catalogitem.application.port.in.UpdateCatalogItemDependencyUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemDependencyRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependency;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyCycleException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyNotFoundException;
import com.vetsoftware.app.catalogitem.domain.DependencyGraph;
import com.vetsoftware.app.catalogitem.domain.RelationType;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "catalogitem.dependency.update")
@Service
public class UpdateCatalogItemDependencyService implements UpdateCatalogItemDependencyUseCase {

    private final CatalogItemDependencyRepository repository;

    public UpdateCatalogItemDependencyService(CatalogItemDependencyRepository repository) {
        this.repository = repository;
    }

    /**
     * Editar también puede cerrar un ciclo: pasar de {@code RECOMMENDS} a
     * {@code REQUIRES} añade al grafo un arco que antes no estaba.
     *
     * <p>
     * <strong>Y solo se comprueba cuando el arco pasa a serlo.</strong> Si ya era
     * {@code REQUIRES} y sigue siéndolo, el arco está en el grafo que devuelve el
     * repositorio y el detector lo encontraría a través de sí mismo: un falso
     * positivo que dejaría sin poder editar la nota de cualquier dependencia
     * legítima.
     */
    @Override
    @Transactional
    public CatalogItemDependencyDto execute(UpdateCatalogItemDependencyCommand command) {
        CatalogItemDependency dependency = repository.findById(command.id())
                .orElseThrow(() -> new CatalogItemDependencyNotFoundException(command.id()));
        if (!dependency.getCatalogItemId().equals(command.catalogItemId())) {
            throw new CatalogItemDependencyNotFoundException(command.id());
        }

        boolean becomesRequires = command.relationType() == RelationType.REQUIRES
                && dependency.getRelationType() != RelationType.REQUIRES;

        dependency.update(command.relationType(), command.note());

        if (becomesRequires) {
            DependencyGraph graph = DependencyGraph.ofRequires(repository.findAllRequiresEdges());
            List<Long> cycle = graph.cycleClosedBy(dependency.getCatalogItemId(),
                    dependency.getRelatedItemId());
            if (!cycle.isEmpty()) {
                throw new CatalogItemDependencyCycleException(cycle);
            }
        }
        return CatalogItemDependencyDto.from(repository.save(dependency));
    }
}
