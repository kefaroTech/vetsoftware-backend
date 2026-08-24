package com.vetsoftware.app.catalogitem.application.usecase;

import com.vetsoftware.app.catalogitem.application.port.in.DeleteCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.out.BundleComponentRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemDependencyRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemSubModuleRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemHasActiveChildrenException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baja lógica de un artículo, con las tres guardas que la base no da.
 *
 * <p>
 * Las FK del modelo son {@code ON DELETE RESTRICT}, pero aquí no se borra: se
 * pone {@code enabled = false}. La fila sigue ahí, así que InnoDB no tiene nada
 * que rechazar y las tres tablas hijas se quedarían apuntando a un artículo que
 * la aplicación ya no ve — el configurador seguiría resolviendo un
 * {@code REQUIRES} contra un artículo retirado, y un paquete seguiría diciendo
 * que trae una pieza que no se vende.
 */
@Observed(name = "catalogitem.delete")
@Service
public class DeleteCatalogItemService implements DeleteCatalogItemUseCase {

    private final CatalogItemRepository repository;
    private final CatalogItemSubModuleRepository subModuleRepository;
    private final CatalogItemDependencyRepository dependencyRepository;
    private final BundleComponentRepository bundleComponentRepository;

    public DeleteCatalogItemService(CatalogItemRepository repository,
            CatalogItemSubModuleRepository subModuleRepository,
            CatalogItemDependencyRepository dependencyRepository,
            BundleComponentRepository bundleComponentRepository) {
        this.repository = repository;
        this.subModuleRepository = subModuleRepository;
        this.dependencyRepository = dependencyRepository;
        this.bundleComponentRepository = bundleComponentRepository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new CatalogItemNotFoundException(id));
        if (subModuleRepository.existsActiveByCatalogItemId(id)) {
            throw new CatalogItemHasActiveChildrenException(id, "catalogItemSubModule");
        }
        if (dependencyRepository.existsActiveInvolving(id)) {
            throw new CatalogItemHasActiveChildrenException(id, "catalogItemDependency");
        }
        if (bundleComponentRepository.existsActiveInvolving(id)) {
            throw new CatalogItemHasActiveChildrenException(id, "bundleComponent");
        }
        repository.delete(id);
    }
}
