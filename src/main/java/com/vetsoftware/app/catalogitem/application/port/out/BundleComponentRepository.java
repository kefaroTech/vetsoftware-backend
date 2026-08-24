package com.vetsoftware.app.catalogitem.application.port.out;

import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.domain.BundleComponent;
import java.util.List;
import java.util.Optional;

public interface BundleComponentRepository {

    BundleComponent save(BundleComponent component);

    Optional<BundleComponent> findById(Long id);

    List<BundleComponent> findAllByBundleItemId(Long bundleItemId);

    void delete(Long id);

    int reactivate(Long id);

    /** El par, ignorando el borrado lógico. Ver {@link LinkStateDto}. */
    Optional<LinkStateDto> findAnyByPair(Long bundleItemId, Long componentItemId);

    /** Si el artículo es paquete de algo o pieza de algún paquete vivo. */
    boolean existsActiveInvolving(Long catalogItemId);
}
