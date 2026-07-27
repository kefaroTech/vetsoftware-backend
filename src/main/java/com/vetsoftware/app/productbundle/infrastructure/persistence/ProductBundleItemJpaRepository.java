package com.vetsoftware.app.productbundle.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductBundleItemJpaRepository extends JpaRepository<ProductBundleItemJpaEntity, Long> {
    @EntityGraph(attributePaths = {"presentation", "presentation.product", "presentation.unitMeasure"})
    List<ProductBundleItemJpaEntity> findAllByCompany_IdAndBundle_IdOrderByDisplayOrderAsc(
        Long companyId, Long bundleId);
}
