package com.vetsoftware.app.productbundle.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductBundleJpaRepository extends JpaRepository<ProductBundleJpaEntity, Long> {
    @EntityGraph(attributePaths = {"company", "unitMeasure"})
    Optional<ProductBundleJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"unitMeasure"})
    List<ProductBundleJpaEntity> findAllByCompany_IdOrderByNameAsc(Long companyId);

    boolean existsByCompany_IdAndCode(Long companyId, String code);
    boolean existsByCompany_IdAndName(Long companyId, String name);
}
