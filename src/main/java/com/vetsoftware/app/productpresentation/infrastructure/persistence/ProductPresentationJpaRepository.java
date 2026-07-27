package com.vetsoftware.app.productpresentation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPresentationJpaRepository extends JpaRepository<ProductPresentationJpaEntity, Long> {
    @EntityGraph(attributePaths = {"company", "product", "unitMeasure"})
    Optional<ProductPresentationJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"product", "unitMeasure"})
    List<ProductPresentationJpaEntity>
        findAllByCompany_IdAndProduct_IdOrderByDefaultPresentationDescNameAsc(Long companyId, Long productId);

    Optional<ProductPresentationJpaEntity>
        findByCompany_IdAndProduct_IdAndDefaultPresentationTrue(Long companyId, Long productId);

    boolean existsByCompany_IdAndProduct_IdAndName(Long companyId, Long productId, String name);

    boolean existsByCompany_IdAndProduct_IdAndNameAndIdNot(
        Long companyId, Long productId, String name, Long id);
}
