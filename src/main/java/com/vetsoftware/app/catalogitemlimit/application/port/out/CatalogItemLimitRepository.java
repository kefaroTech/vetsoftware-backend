package com.vetsoftware.app.catalogitemlimit.application.port.out;

import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimit;
import java.util.List;
import java.util.Optional;

/** Adaptador de salida de los techos de fábrica. */
public interface CatalogItemLimitRepository {

    CatalogItemLimit save(CatalogItemLimit limit);

    Optional<CatalogItemLimit> findById(Long id);

    /**
     * La carga acotada por el artículo del que cuelga el techo.
     *
     * <p>
     * <strong>Es la que usa la edición</strong>, y no la ancha de arriba: el
     * {@code id} lo escribe el cliente en la URL y el artículo también, así que
     * cargar solo por el primero deja que la ruta del artículo 9 edite el techo del
     * 7. Aquí no hay empresa que proteger —esta tabla es catálogo global— pero el
     * criterio es el mismo que el de la familia «por id»: si la ruta nombra un
     * padre, el {@code WHERE} lo nombra también o la ruta no significa nada.
     */
    Optional<CatalogItemLimit> findByIdAndCatalogItemId(Long id, Long catalogItemId);

    Optional<CatalogItemLimit> findByCatalogItemIdAndLimitDimensionId(Long catalogItemId,
            Long limitDimensionId);

    boolean existsByCatalogItemIdAndLimitDimensionId(Long catalogItemId, Long limitDimensionId);

    List<CatalogItemLimit> findAllByCatalogItemId(Long catalogItemId);
}
